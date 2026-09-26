package com.calisvision.test

/** Marks instrumented tests that need videos pushed to [TestVideos.dir]; skip when absent. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequiresVideo
