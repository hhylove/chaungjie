package com.chuangjie.module.assistant.service;

import com.chuangjie.framework.tenant.core.context.TenantContextHolder;
import com.chuangjie.framework.common.exception.ServiceException;
import com.chuangjie.module.assistant.dal.AssistantRepository;
import com.chuangjie.module.system.dal.dataobject.user.AdminUserDO;
import com.chuangjie.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AssistantServiceTest {

    private final AssistantRepository repository = mock(AssistantRepository.class);
    private final LocalModelClient model = mock(LocalModelClient.class);
    private final AdminUserService users = mock(AdminUserService.class);
    private final KnowledgeService knowledge = mock(KnowledgeService.class);
    private final AssistantService service = new AssistantService(repository, model, users, knowledge);

    @BeforeEach
    void setTenant() { TenantContextHolder.setTenantId(10L); }

    @AfterEach
    void clearTenant() { TenantContextHolder.clear(); }

    @Test
    void availabilityRequiresSameTenantEnabledAccountAndGrant() {
        when(repository.getConfig(10L)).thenReturn(new AssistantRepository.Config(10L, true,
                "http://127.0.0.1:8000/v1", "local", 30, 10));
        when(repository.hasGrant(10L, 9L)).thenReturn(true);
        AdminUserDO user = new AdminUserDO();
        user.setId(9L);
        user.setStatus(0);
        user.setTenantId(10L);
        when(users.getUser(9L)).thenReturn(user);
        assertTrue(service.available(9L));
        user.setTenantId(11L);
        assertFalse(service.available(9L));
        user.setTenantId(10L);
        user.setStatus(1);
        assertFalse(service.available(9L));
        verify(repository, times(3)).hasGrant(10L, 9L);
    }

    @Test
    void disabledTenantDeniesUse() {
        when(repository.getConfig(10L)).thenReturn(new AssistantRepository.Config(10L, false,
                null, null, 30, 10));
        assertFalse(service.available(9L));
    }

    @Test
    void revokedDuringModelCallDoesNotReturnOrPersistAnswer() {
        AssistantRepository.Config config = new AssistantRepository.Config(10L, true,
                "http://127.0.0.1:8000/v1", "local", 30, 10);
        when(repository.getConfig(10L)).thenReturn(config);
        when(repository.hasGrant(10L, 9L)).thenReturn(true, false);
        when(repository.reserveRequest(10L, 9L)).thenReturn(1);
        AdminUserDO user = new AdminUserDO();
        user.setId(9L);
        user.setStatus(0);
        user.setTenantId(10L);
        when(users.getUser(9L)).thenReturn(user);
        when(model.ask(eq(config), anyList(), eq("hello"))).thenReturn("answer");
        when(knowledge.search(9L, "hello")).thenReturn(java.util.List.of());

        assertThrows(ServiceException.class, () -> service.ask(9L, null, "hello"));
        verify(repository, never()).createConversation(anyLong(), anyLong(), anyString());
        verify(repository, never()).addMessage(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void revokedKnowledgeDocumentDoesNotReturnOrPersistAnswer() {
        AssistantRepository.Config config = new AssistantRepository.Config(10L, true,
                "http://127.0.0.1:8000/v1", "local", 30, 10);
        when(repository.getConfig(10L)).thenReturn(config);
        when(repository.hasGrant(10L, 9L)).thenReturn(true);
        when(repository.reserveRequest(10L, 9L)).thenReturn(1);
        AdminUserDO user = new AdminUserDO();
        user.setId(9L);
        user.setStatus(0);
        user.setTenantId(10L);
        when(users.getUser(9L)).thenReturn(user);
        var evidence = new KnowledgeService.Evidence(51L, "规则.pdf", 1, "内部规则", 0.8);
        when(knowledge.search(9L, "规则是什么")).thenReturn(java.util.List.of(evidence));
        when(model.ask(eq(config), anyList(), eq("规则是什么"), anyString())).thenReturn("内部规则");
        when(knowledge.stillAccessible(9L, java.util.List.of(evidence))).thenReturn(false);

        assertThrows(ServiceException.class, () -> service.ask(9L, null, "规则是什么"));
        verify(repository, never()).addKnowledgeAnswer(anyLong(), anyLong(), anyString(), anyList());
    }

    @Test
    void revokedKnowledgeAnswerIsFilteredFromHistory() {
        when(repository.getConfig(10L)).thenReturn(new AssistantRepository.Config(10L, true,
                "http://127.0.0.1:8000/v1", "local", 30, 10));
        when(repository.hasGrant(10L, 9L)).thenReturn(true);
        AdminUserDO user = new AdminUserDO();
        user.setId(9L);
        user.setStatus(0);
        user.setTenantId(10L);
        when(users.getUser(9L)).thenReturn(user);
        when(repository.ownsConversation(10L, 9L, 30L)).thenReturn(true);
        when(repository.listMessages(10L, 30L)).thenReturn(java.util.List.of(
                java.util.Map.of("id", 1L, "role", "assistant", "content", "旧答案"),
                java.util.Map.of("id", 2L, "role", "assistant", "content", "公开答案")));
        when(repository.messageReferences(10L, java.util.List.of(1L, 2L)))
                .thenReturn(java.util.Map.of(1L, java.util.List.of(new AssistantRepository.KnowledgeRef(51L, 1))));
        when(knowledge.accessibleDocumentVersions(9L)).thenReturn(java.util.Map.of(51L, 2));

        var messages = service.myMessages(9L, 30L);
        assertEquals(1, messages.size());
        assertEquals("公开答案", messages.get(0).get("content"));
    }
}
