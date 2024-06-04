# Intellij IDEA configuration

`ktlint` strives to prevent code formatting conflicts with IntelliJ IDEA / Android Studio as much as possible. In some cases, `ktlint` deliberately deviates from IDEA formatting.

## Preventing conflicts

Many conflicts can be prevented by setting following `.editorconfig` settings:
```
root = true

[*]
insert_final_newline = true

[{*.kt,*.kts}]
ij_kotlin_code_style_defaults = KOTLIN_OFFICIAL

#  Disable wildcard imports entirely
ij_kotlin_name_count_to_use_star_import = 2147483647
ij_kotlin_name_count_to_use_star_import_for_members = 2147483647
ij_kotlin_packages_to_use_import_on_demand = unset
```

Conflicts between `ktlint` and IDEA formatting can also be resolved by using the [ktlint-intellij-plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) (or install via Intellij IDEA plugin marketplace) in `distract free` mode. In this mode, the plugin formats your code with `ktlint` while you're editing the code.
 
# Cleaning up old XML configuration settings

Projects which have been created with (old)er versions of Intellij IDEA might still contain XML configuration regarding code styling. It is advised to remove the directory `.idea/codeStyles` whenever it still exists in your project directory.
