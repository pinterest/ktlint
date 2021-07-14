JAR=$(\find ktlint/build/libs | \grep -E "all" | head)
if [ ! -f $JAR ]; then
    echo "File not found! run ./gradlew clean publishMavenPublicationToMavenCentralRepository --no-daemon --no-parallel"
    exit 1;
fi

base_dir=$(pwd);
cd $1;
shift;
java -jar $base_dir/$JAR  $@;
