interface FooService {

    fun foo1(
        @Path("fooId") fooId: String,
        @Path("bar") bar: String,
        @Body body: Foo
    ): Completable

    fun foo2(@Query("include") include: String? = null, @QueryMap fields: Map<String, String> = emptyMap()): Single

    fun foo3(@Path("fooId") fooId: String): Completable
}