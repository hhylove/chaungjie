<script setup lang="ts">
import QRCode from 'qrcode'
import * as ProcessInstanceApi from '@/api/bpm/processInstance'
import * as AreaApi from '@/api/system/area'
import * as DeptApi from '@/api/system/dept'
import * as TenantApi from '@/api/system/tenant'
import * as UserApi from '@/api/system/user'
import { getLoginForm, getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'
import { formatDate } from '@/utils/formatTime'
import {
  DICT_TYPE,
  getBoolDictOptions,
  getDictLabel,
  getDictOptions,
  getIntDictOptions,
  getStrDictOptions
} from '@/utils/dict'
import { decodeFields } from '@/utils/formCreate'
import { registerComponent } from '@/utils/routerHelper'

interface FormFieldItem {
  html: string
  id: string
  name: string
}

interface FormFieldOption {
  label?: string
  value?: unknown
}

type FormFieldRule = Record<string, unknown> & {
  field?: string
  options?: FormFieldOption[]
  props?: Record<string, unknown>
  title?: string
  type?: string
}

type PrintableRecord = Record<string, unknown>

interface AreaNode {
  children?: AreaNode[]
  id?: number
  name: string
}

interface PrintLookupMaps {
  areaMap: Map<string, string>
  deptMap: Map<string, string>
  userMap: Map<string, string>
}

const userStore = useUserStore()

const visible = ref(false)
const loading = ref(false)

const printData = ref()
const userName = computed(() => userStore.user.nickname ?? '')
const printTime = ref(formatDate(new Date(), 'YYYY/M/D HH:mm'))
const formFields = ref<FormFieldItem[]>([])
const printDataMap = ref<Record<string, string>>({})
const BusinessFormComponent = shallowRef<any>()
const companyName = ref('')
const qrCodeUrl = ref('')

const open = async (id: string) => {
  loading.value = true
  try {
    printData.value = await ProcessInstanceApi.getProcessInstancePrintData(id)
    printTime.value = formatDate(new Date(), 'YYYY/M/D HH:mm')
    initPrintDataMap()
    await Promise.all([parseFormFields(), loadCompanyName(), loadQrCode(id)])
    initBusinessFormComponent()
  } finally {
    loading.value = false
  }
  visible.value = true
}
defineExpose({ open })

const initBusinessFormComponent = () => {
  const businessFormPath =
    printData.value?.processInstance?.processDefinition?.formCustomViewPath || ''
  BusinessFormComponent.value = businessFormPath ? registerComponent(businessFormPath) : undefined
}

const parseFormFields = async () => {
  if (!printData.value) return

  const formFieldsObj = decodeFields(
    printData.value.processInstance.processDefinition?.formFields || []
  ) as FormFieldRule[]
  const processVariables = printData.value.processInstance.formVariables ?? {}
  const lookupMaps = await loadPrintLookupMaps(formFieldsObj)
  const res: FormFieldItem[] = []

  for (const item of formFieldsObj) {
    const fieldKey = String(item.field ?? '')
    const id = fieldKey
    const name = String(item.title ?? fieldKey)
    const variable = processVariables[fieldKey]
    const html = formatPrintField(item, variable, lookupMaps)

    printDataMap.value[fieldKey] = html
    res.push({ id, name, html })
  }
  formFields.value = res
}

const getRuleProp = (rule: FormFieldRule, key: string) => {
  return rule?.[key] ?? rule?.props?.[key]
}

const isPrintableRecord = (value: unknown): value is PrintableRecord => {
  return typeof value === 'object' && value !== null
}

const getRecordValue = (record: PrintableRecord, key: string) => {
  return record[key]
}

const isNotEmptyString = (value: string) => value.length > 0

const isEmptyValue = (value: unknown) => value === undefined || value === null || value === ''

const toValueArray = (value: unknown) => {
  if (Array.isArray(value)) {
    return value
  }
  if (isEmptyValue(value)) {
    return []
  }
  return [value]
}

const escapeHtml = (value: unknown) => {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const tryFormatDate = (value: unknown) => {
  if (isEmptyValue(value)) {
    return ''
  }
  const formatted = formatDate(value as Date | number | string)
  return formatted === 'Invalid Date' ? escapeHtml(value) : formatted
}

const formatDateValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return value.map((item) => tryFormatDate(item)).join(' ~ ')
  }
  return tryFormatDate(value)
}

const formatPrimitiveValue = (value: unknown): string => {
  if (isEmptyValue(value)) {
    return ''
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => formatPrimitiveValue(item))
      .filter((s) => isNotEmptyString(s))
      .join(', ')
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  if (isPrintableRecord(value)) {
    const displayValue =
      getRecordValue(value, 'label') ??
      getRecordValue(value, 'name') ??
      getRecordValue(value, 'url') ??
      getRecordValue(value, 'value') ??
      JSON.stringify(value)
    return escapeHtml(displayValue)
  }
  return escapeHtml(value)
}

const createImageHtml = (url: string) => {
  const imgEl = document.createElement('img')
  imgEl.setAttribute('src', url)
  imgEl.setAttribute('style', 'max-width: 600px; max-height: 300px;')
  return imgEl.outerHTML
}

const renderImageListHtml = (value: unknown) => {
  return toValueArray(value)
    .map((item) => {
      let url: string | undefined
      if (typeof item === 'string') {
        url = item
      } else if (isPrintableRecord(item)) {
        const recordUrl = getRecordValue(item, 'url')
        url = recordUrl ? String(recordUrl) : undefined
      }
      return url ? createImageHtml(url) : ''
    })
    .filter((s) => isNotEmptyString(s))
    .join('<br/>')
}

const createFileLinkHtml = (file: unknown) => {
  const record = isPrintableRecord(file) ? file : undefined
  const recordUrl = record ? getRecordValue(record, 'url') : undefined
  const url = typeof file === 'string' ? file : String(recordUrl ?? '')
  if (!url) {
    return ''
  }
  const linkEl = document.createElement('a')
  linkEl.setAttribute('href', url)
  linkEl.setAttribute('target', '_blank')
  linkEl.setAttribute('rel', 'noopener noreferrer')
  const fallbackName = url.slice(Math.max(0, url.lastIndexOf('/') + 1)) || url
  const recordName = record ? getRecordValue(record, 'name') : undefined
  linkEl.textContent = recordName ? String(recordName) : fallbackName
  return linkEl.outerHTML
}

const renderFileListHtml = (value: unknown) => {
  return toValueArray(value)
    .map((item) => createFileLinkHtml(item))
    .filter((s) => isNotEmptyString(s))
    .join('<br/>')
}

const mapValuesWithOptions = (value: unknown, options: FormFieldOption[] = []) => {
  const values = toValueArray(value)
  const labels = values
    .map((item) => {
      const matched = options.find(
        (option) => option?.value === item || String(option?.value ?? '') === String(item)
      )
      return escapeHtml(matched?.label ?? String(item))
    })
    .filter((s) => isNotEmptyString(s))
  return labels.join(', ')
}

const flattenAreaTree = (list: AreaNode[] = [], map: Map<string, string> = new Map()) => {
  list.forEach((item) => {
    if (item.id !== undefined) {
      map.set(String(item.id), item.name)
    }
    if (Array.isArray(item.children) && item.children.length > 0) {
      flattenAreaTree(item.children, map)
    }
  })
  return map
}

const mapValueWithLabelMap = (value: unknown, labelMap: Map<string, string>, separator = ', ') => {
  const values = toValueArray(value)
  const labels = values
    .map((item) => escapeHtml(labelMap.get(String(item)) ?? String(item)))
    .filter((s) => isNotEmptyString(s))
  return labels.length > 0 ? labels.join(escapeHtml(separator)) : formatPrimitiveValue(values)
}

const getTypedDictOptions = (dictType: string, valueType: string) => {
  switch (valueType) {
    case 'bool':
    case 'boolean':
      return getBoolDictOptions(dictType)
    case 'int':
    case 'number':
      return getIntDictOptions(dictType)
    case 'str':
    case 'string':
      return getStrDictOptions(dictType)
    default:
      return getDictOptions(dictType)
  }
}

const loadPrintLookupMaps = async (formFieldsObj: FormFieldRule[]) => {
  const hasAreaSelect = formFieldsObj.some((item) => item.type === 'AreaSelect')
  const hasUserSelect = formFieldsObj.some((item) => item.type === 'UserSelect')
  const hasDeptSelect = formFieldsObj.some((item) => item.type === 'DeptSelect')

  const [areaList, userList, deptList] = await Promise.all([
    hasAreaSelect ? AreaApi.getAreaTree() : Promise.resolve([]),
    hasUserSelect ? UserApi.getSimpleUserList() : Promise.resolve([]),
    hasDeptSelect ? DeptApi.getSimpleDeptList() : Promise.resolve([])
  ])

  return {
    areaMap: flattenAreaTree(areaList as AreaNode[]),
    deptMap: new Map((deptList ?? []).map((item) => [String(item.id), item.name] as const)),
    userMap: new Map(
      (userList ?? []).map((item) => [String(item.id), item.nickname ?? item.username] as const)
    )
  } satisfies PrintLookupMaps
}

const formatPrintField = (rule: FormFieldRule, value: unknown, lookupMaps: PrintLookupMaps) => {
  const type = String(rule.type ?? '')

  switch (type) {
    case 'AreaSelect': {
      const separator = String(getRuleProp(rule, 'separator') || '/')
      return mapValueWithLabelMap(value, lookupMaps.areaMap, separator)
    }
    case 'cascader':
    case 'checkbox':
    case 'radio':
    case 'select':
    case 'treeSelect': {
      const options = getRuleProp(rule, 'options')
      return Array.isArray(options) && options.length > 0
        ? mapValuesWithOptions(value, options as FormFieldOption[])
        : formatPrimitiveValue(value)
    }
    case 'date':
    case 'DatePicker':
    case 'datePicker':
    case 'daterange':
    case 'datetime':
    case 'datetimerange':
    case 'month':
    case 'monthrange':
    case 'RangePicker':
    case 'rangePicker':
    case 'TimePicker':
    case 'timePicker':
    case 'TimeRangePicker':
    case 'timeRangePicker':
      return formatDateValue(value)
    case 'DeptSelect': {
      if (String(getRuleProp(rule, 'returnType')) === 'name') {
        return formatPrimitiveValue(value)
      }
      return mapValueWithLabelMap(value, lookupMaps.deptMap)
    }
    case 'DictSelect': {
      const dictType = getRuleProp(rule, 'dictType')
      if (typeof dictType !== 'string' || !dictType) {
        return formatPrimitiveValue(value)
      }
      const valueType = String(getRuleProp(rule, 'valueType') ?? '')
      return mapValuesWithOptions(value, getTypedDictOptions(dictType, valueType))
    }
    case 'FileUpload':
    case 'UploadFile':
      return renderFileListHtml(value)
    case 'IframeComponent': {
      const propsObj = rule.props
      const propsUrl = isPrintableRecord(propsObj)
        ? String(getRecordValue(propsObj, 'url') ?? '')
        : ''
      const iframeUrl = isEmptyValue(value) ? propsUrl : String(value ?? '')
      return iframeUrl ? createFileLinkHtml(iframeUrl) : ''
    }
    case 'ImagesUpload':
    case 'ImageUpload':
    case 'UploadImg':
    case 'UploadImgs':
      return renderImageListHtml(value)
    case 'switch': {
      if (isEmptyValue(value)) return '否'
      const checkedVal = getRuleProp(rule, 'checkedValue') ?? getRuleProp(rule, 'activeValue')
      const isChecked =
        checkedVal !== undefined && checkedVal !== null ? value === checkedVal : Boolean(value)
      return isChecked ? '是' : '否'
    }
    case 'Editor':
    case 'Tinymce':
      return isEmptyValue(value) ? '' : String(value)
    case 'UserSelect': {
      if (String(getRuleProp(rule, 'returnType')) === 'name') {
        return formatPrimitiveValue(value)
      }
      return mapValueWithLabelMap(value, lookupMaps.userMap)
    }
    default:
      return formatPrimitiveValue(value)
  }
}

const initPrintDataMap = () => {
  if (!printData.value) return

  printDataMap.value['startUser'] = printData.value.processInstance.startUser?.nickname || ''
  printDataMap.value['startUserDept'] = printData.value.processInstance.startUser?.deptName || ''
  printDataMap.value['processName'] = printData.value.processInstance.name
  printDataMap.value['processNum'] = String(printData.value.processInstance.id ?? '')
  printDataMap.value['startTime'] = formatDate(
    printData.value.processInstance.startTime,
    'YYYY/M/D HH:mm'
  )
  printDataMap.value['endTime'] = formatDate(
    printData.value.processInstance.endTime,
    'YYYY/M/D HH:mm'
  )
  printDataMap.value['processStatus'] = String(
    getDictLabel(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS, printData.value.processInstance.status) ??
      ''
  )
  printDataMap.value['printUser'] = userName.value
  printDataMap.value['printTime'] = printTime.value
}

const loadCompanyName = async () => {
  const loginName = getLoginForm()?.tenantName
  if (loginName) companyName.value = loginName
  const tenantId = getTenantId()
  if (!tenantId) return
  try {
    const tenant = await TenantApi.getTenant(tenantId)
    if (tenant?.name) companyName.value = tenant.name
  } catch {
    // 无租户查询权限时保留登录页记住的租户名
  }
}

const loadQrCode = async (id: string) => {
  const url = `${window.location.origin}/bpm/process-instance/detail?id=${id}`
  try {
    qrCodeUrl.value = await QRCode.toDataURL(url, { margin: 1, width: 96 })
  } catch {
    qrCodeUrl.value = ''
  }
}

const processStatusText = computed(() => {
  return (
    getDictLabel(DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS, printData.value?.processInstance?.status) ||
    ''
  )
})

const formFieldRows = computed(() => {
  const rows: FormFieldItem[][] = []
  for (let i = 0; i < formFields.value.length; i += 2) {
    rows.push(formFields.value.slice(i, i + 2))
  }
  return rows
})

const taskNodeText = (task: any) => {
  const status = task?.status != null ? getDictLabel(DICT_TYPE.BPM_TASK_STATUS, task.status) : ''
  return [task?.name, status].filter(Boolean).join(' ') || '-'
}

const taskHandlerText = (task: any) => {
  return task?.assigneeUser?.nickname || task?.ownerUser?.nickname || task?.assignee || ''
}

const taskRecordText = (task: any) => {
  const text = task?.reason || task?.description || ''
  return text || '/'
}

const getPrintTemplateHTML = () => {
  if (!printData.value?.printTemplateHtml) return ''

  const parser = new DOMParser()
  const doc = parser.parseFromString(printData.value.printTemplateHtml, 'text/html')
  // table 添加border
  const tables = doc.querySelectorAll('table')
  tables.forEach((item) => {
    item.setAttribute('border', '1')
    item.setAttribute('style', (item.getAttribute('style') || '') + 'border-collapse:collapse;')
  })
  // 替换 mentions
  const mentions = doc.querySelectorAll('[data-w-e-type="mention"]')
  mentions.forEach((item) => {
    const mentionId = JSON.parse(decodeURIComponent(item.getAttribute('data-info') ?? ''))['id']
    item.innerHTML = printDataMap.value[mentionId] ?? ''
  })
  // 替换流程记录
  const processRecords = doc.querySelectorAll('[data-w-e-type="process-record"]')
  const processRecordTable: Element = document.createElement('table')
  if (processRecords.length > 0) {
    // 构建流程记录html
    processRecordTable.setAttribute('border', '1')
    processRecordTable.setAttribute('style', 'width:100%;border-collapse:collapse;')
    const headTr = document.createElement('tr')
    const headers = [
      { text: '审批节点', colspan: '1' },
      { text: '处理人', colspan: '1' },
      { text: '操作记录', colspan: '2' }
    ]
    headers.forEach((header) => {
      const td = document.createElement('td')
      td.setAttribute('colspan', header.colspan)
      td.setAttribute('style', 'text-align:center;background:#f5f6f7;font-weight:600;')
      td.textContent = header.text
      headTr.appendChild(td)
    })
    processRecordTable.appendChild(headTr)
    printData.value.tasks.forEach((item) => {
      const tr = document.createElement('tr')
      const cells = [taskNodeText(item), taskHandlerText(item) || '-', taskRecordText(item)]
      cells.forEach((text, index) => {
        const td = document.createElement('td')
        if (index === 2) td.setAttribute('colspan', '2')
        td.textContent = text
        tr.appendChild(td)
      })
      processRecordTable.appendChild(tr)
    })
  }
  processRecords.forEach((item) => {
    item.innerHTML = processRecordTable.outerHTML
  })
  // 返回 html
  return doc.body.innerHTML
}

const printObj = ref({
  id: 'printDivTag',
  popTitle: '&nbsp',
  extraCss: '/print.css',
  extraHead: '',
  zIndex: 20003
})
</script>

<template>
  <el-dialog v-loading="loading" v-model="visible" :show-close="false">
    <div id="printDivTag" style="word-break: break-all">
      <div v-if="printData.printTemplateEnable" v-html="getPrintTemplateHTML()"></div>
      <div v-else class="print-sheet">
        <h2 class="print-sheet__title">{{ printData.processInstance.name }}</h2>
        <div class="print-sheet__meta">
          <span>{{ companyName }}</span>
          <span>审批编号：{{ printData.processInstance.id }}</span>
        </div>
        <table class="print-sheet__table" border="1">
          <tbody>
            <tr>
              <td class="label">申请人</td>
              <td>{{ printData.processInstance.startUser?.nickname }}</td>
              <td class="label">申请人部门</td>
              <td>{{ printData.processInstance.startUser?.deptName }}</td>
            </tr>
            <tr>
              <td class="label">提交时间</td>
              <td>{{ formatDate(printData.processInstance.startTime, 'YYYY/M/D HH:mm') }}</td>
              <td class="label">当前审批状态</td>
              <td>{{ processStatusText }}</td>
            </tr>
            <tr>
              <td class="section" colspan="4">申请内容</td>
            </tr>
            <tr v-for="(row, rowIndex) in formFieldRows" :key="rowIndex">
              <template v-if="row.length === 2">
                <td class="label">{{ row[0].name }}</td>
                <td><div v-html="row[0].html"></div></td>
                <td class="label">{{ row[1].name }}</td>
                <td><div v-html="row[1].html"></div></td>
              </template>
              <template v-else>
                <td class="label">{{ row[0].name }}</td>
                <td colspan="3"><div v-html="row[0].html"></div></td>
              </template>
            </tr>
            <tr v-if="formFields.length === 0 && !BusinessFormComponent">
              <td colspan="4" class="empty">无</td>
            </tr>
            <tr>
              <td class="section" colspan="4">审批流程-{{ processStatusText || '审批' }}</td>
            </tr>
            <tr>
              <td class="label">审批节点</td>
              <td class="label">处理人</td>
              <td class="label" colspan="2">操作记录</td>
            </tr>
            <tr v-for="item in printData.tasks" :key="item.id">
              <td>{{ taskNodeText(item) }}</td>
              <td>
                {{ taskHandlerText(item) || '-' }}
                <div v-if="item.signPicUrl">
                  <img class="sign" :src="item.signPicUrl" alt="" />
                </div>
              </td>
              <td colspan="2">{{ taskRecordText(item) }}</td>
            </tr>
            <tr>
              <td class="label">备注信息</td>
              <td colspan="3">{{ printData.processInstance.remark || '' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="BusinessFormComponent && formFields.length === 0" class="print-sheet__business">
          <component
            :is="BusinessFormComponent"
            :id="printData.processInstance.businessKey"
            :readonly="true"
            :print-mode="true"
          />
        </div>
        <div class="print-sheet__footer">
          <div class="print-sheet__qr">
            <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="" />
            <span>使用手机扫一扫</span>
          </div>
          <div class="print-sheet__printer">
            <div>打印日期：{{ printTime }}</div>
            <div>打印人：{{ userName }}</div>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取 消</el-button>
        <el-button type="primary" v-print="printObj"> 打 印</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style>
.print-sheet {
  color: #1f2329;
  font-size: 14px;
}

.print-sheet__title {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 700;
  text-align: center;
}

.print-sheet__meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.print-sheet__table {
  width: 100%;
  border-collapse: collapse;
}

.print-sheet__table td {
  padding: 8px 10px;
  border: 1px solid #1f2329;
  vertical-align: middle;
}

.print-sheet__table .label {
  width: 18%;
  font-weight: 600;
  text-align: center;
  background: #f5f6f7;
}

.print-sheet__table .section {
  font-weight: 600;
  text-align: center;
  background: #f5f6f7;
}

.print-sheet__table .empty {
  color: #8a909c;
  text-align: center;
}

.print-sheet__business {
  margin-top: 16px;
}

.print-sheet__footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 16px;
}

.print-sheet__qr {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #646a73;
}

.print-sheet__qr img {
  width: 72px;
  height: 72px;
}

.print-sheet__printer {
  font-size: 13px;
  line-height: 1.8;
  text-align: right;
}

.print-sheet .sign {
  width: 90px;
  height: 40px;
}

/* 修复打印只显示一页 */
@media print {
  @page {
    size: auto;
  }

  body,
  html,
  div {
    height: auto !important;
  }
}
</style>
