def buildLog = new File(basedir, "build.log").text;
def cachedMatches = buildLog =~ /INVOKER_IT_TEST_MARKER_CACHED/;
def uncachedMatches = buildLog =~ /INVOKER_IT_TEST_MARKER_NONCACHED/;

return 1 == cachedMatches.size() && 2 == uncachedMatches.size();