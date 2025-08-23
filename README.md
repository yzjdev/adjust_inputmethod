## 简单记录一下

主流输入法简单适配  支持全选 复制 剪切 粘贴

某度输入法字符上限259_795，超过该值就会卡顿(仅自己设备测试)

设置后光标超过该值，右和左无法使用，选择状态下可用

选择键盘下的`←`调用`setSelection(0,0)`   `→`调用`setSelection(maxChars,maxChars)` maxChars不设置，默认为ExtractedText.text.length()

且必须实现`getExtractedText`  `getTextBeforeCursor`  `getTextAfterCursor`才能使光标移动