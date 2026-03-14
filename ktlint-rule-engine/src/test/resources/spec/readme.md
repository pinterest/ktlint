The spec files in this directory contain the UTF BOM character \uFEFF

The files have been created with Zsh terminal commands below:
```shell
echo -e "\uFEFF// Although probably not visible in your editor, this file starts with a UTF8 BOM unicode character\n    // The indent before this comment needs to be removed by ktlint format" > format-unicode-bom-at-start-of-file-before-format.kt.spec
echo -e "\uFEFF// Although probably not visible in your editor, this file starts with a UTF8 BOM unicode character\n// The indent before this comment needs to be removed by ktlint format" > format-unicode-bom-at-start-
of-file-after-format.kt.spec

echo -e "// Although probably not visible in your editor, this file contains at UTF BOM --> \uFEFF <--\n    // The indent before this comment needs to be removed by ktlint format" > do-not-format-unicode-bom-when-not-at-start-of-file-before-ktlint-format.kt.spec
echo -e "// Although probably not visible in your editor, this file contains at UTF BOM --> \uFEFF <--\n// The indent before this comment needs to be removed by ktlint format" > do-not-format-unicode-bom-when-not-at-st
art-of-file--after-ktlint-format.kt.spec
```

With the `xxd` command the files can be inspected:
```shell
xxd do-not-format-unicode-bom-when-not-at-start-of-file--before-ktlint-format.kt
```
results in output like:
```terminaloutput
00000000: 2f2f 2041 6c74 686f 7567 6820 7072 6f62  // Although prob
00000010: 6162 6c79 206e 6f74 2076 6973 6962 6c65  ably not visible
00000020: 2069 6e20 796f 7572 2065 6469 746f 722c   in your editor,
00000030: 2074 6869 7320 6669 6c65 2063 6f6e 7461   this file conta
00000040: 696e 7320 6174 2055 5446 2042 4f4d 202d  ins at UTF BOM -
00000050: 2d3e 20ef bbbf 203c 2d2d 0a20 2020 202f  -> ... <--.    /    <<-- The "ef bb" corresponds with the \uFEFF character
00000060: 2f20 5468 6520 696e 6465 6e74 2062 6566  / The indent bef
00000070: 6f72 6520 7468 6973 2063 6f6d 6d65 6e74  ore this comment
00000080: 206e 6565 6473 2074 6f20 6265 2072 656d   needs to be rem
00000090: 6f76 6564 2062 7920 6b74 6c69 6e74 2066  oved by ktlint f
000000a0: 6f72 6d61 740a                           ormat.
```
