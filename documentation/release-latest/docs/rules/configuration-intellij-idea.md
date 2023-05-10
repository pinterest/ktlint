!!! Warning
    `ktlint` strives to prevent code formatting conflicts with IntelliJ IDEA / Android Studio. We recommend using either IDE formatting or `ktlint` formatting. However, if you persist on using both, then please ensure that the formatting settings are aligned as described below.  This reduces the chance that code which is formatted by ktlint conflicts with formatting by the IntelliJ IDEA built-in formatter.

!!! Note
    IntelliJ IDEA supports the [kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). As of version 0.47.x of ktlint, the support to overwrite some configuration files of IntelliJ IDEA has been dropped as it no longer fits the scope of the project. 


Steps:

1. Go to your project directory
2. Create or replace file `.idea/codeStyles/codeStyleConfig.xml` with content below:
   ```xml
   <component name="ProjectCodeStyleConfiguration">
     <state>
       <option name="USE_PER_PROJECT_SETTINGS" value="true" />
     </state>
   </component>
   ```
3. Create or replace file `.idea/codeStyles/Project.xml` with content below:
   ```xml
   <component name="ProjectCodeStyleConfiguration">
     <code_scheme name="Project" version="173">
       <JetCodeStyleSettings>
         <option name="PACKAGES_TO_USE_STAR_IMPORTS">
           <value />
         </option>
         <option name="NAME_COUNT_TO_USE_STAR_IMPORT" value="2147483647" />
         <option name="NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS" value="2147483647" />
         <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
       </JetCodeStyleSettings>
       <codeStyleSettings language="kotlin">
         <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
       </codeStyleSettings>
     </code_scheme>
   </component>
   ```
