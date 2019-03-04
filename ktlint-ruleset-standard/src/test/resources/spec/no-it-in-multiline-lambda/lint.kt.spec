fun main() {
  appendCommaSeparated(properties) {
      val propertyValue = it.get(obj)
      propertyValue.get()
  }

  elements.dropWhile { it !is KDoc }
  appendCommaSeparated(properties) { prop ->
      val propertyValue = prop.get(obj)
  }
}

// expect
// 3:27:Multiline lambda must explicitly name "it" parameter (cannot be auto-corrected)
