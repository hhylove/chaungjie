<script lang="tsx">
import { computed, defineComponent, PropType } from 'vue'
import { isHexColor } from '@/utils/color'
import { DictDataType, getDictOptions } from '@/utils/dict'
import { isArray, isBoolean, isNumber, isString } from '@/utils/is'

const normalizeColorType = (colorType?: string) => {
  if (!colorType || colorType === 'default' || colorType === 'primary') {
    return 'primary'
  }
  return colorType
}

export default defineComponent({
  name: 'DictTag',
  props: {
    type: {
      type: String as PropType<string>,
      required: true
    },
    value: {
      type: [String, Number, Boolean, Array],
      required: true
    },
    // 字符串分隔符 只有当 props.value 传入值为字符串时有效
    separator: {
      type: String as PropType<string>,
      default: ','
    },
    // 每个 tag 之间的间隔，默认为 5px，参考的 el-row 的 gutter
    gutter: {
      type: String as PropType<string>,
      default: '5px'
    }
  },
  setup(props) {
    const valueArr: any = computed(() => {
      // 1. 是 Number 类型和 Boolean 类型的情况
      if (isNumber(props.value) || isBoolean(props.value)) {
        return [String(props.value)]
      }
      // 2. 是字符串（进一步判断是否有包含分隔符号 -> props.sepSymbol ）
      else if (isString(props.value)) {
        return props.value.split(props.separator)
      }
      // 3. 数组
      else if (isArray(props.value)) {
        return props.value.map(String)
      }
      return []
    })
    const renderDictTag = () => {
      if (!props.type) {
        return null
      }
      // 解决自定义字典标签值为零时标签不渲染的问题
      if (props.value === undefined || props.value === null || props.value === '') {
        return null
      }
      const dictOptions = getDictOptions(props.type)

      return (
        <div
          class="dict-tag"
          style={{
            display: 'inline-flex',
            gap: props.gutter,
            justifyContent: 'center',
            alignItems: 'center'
          }}
        >
          {dictOptions.map((dict: DictDataType) => {
            if (!valueArr.value.includes(dict.value)) {
              return null
            }
            const customColor = dict?.cssClass && isHexColor(dict.cssClass) ? dict.cssClass : ''
            const colorType = normalizeColorType(dict?.colorType)
            return (
              <span
                class={[
                  'dict-tag-capsule',
                  customColor ? 'dict-tag-capsule--custom' : `dict-tag-capsule--${colorType}`
                ]}
                style={
                  customColor
                    ? ({
                        '--capsule-custom': customColor,
                        '--capsule-accent': customColor
                      } as Record<string, string>)
                    : undefined
                }
              >
                <span class="dict-tag-capsule__dot" />
                {dict?.label}
              </span>
            )
          })}
        </div>
      )
    }
    return () => renderDictTag()
  }
})
</script>
