# Caching

Akka HTTP のキャッシングサポートは、フューチャーに基づいた軽量かつ高速なインメモリキャッシング機能を提供します。
主なユースケースは、K 型の特定のキーに基づいて、ラップされたオペレーションを 1 回だけ実行し、
同じキーに対する将来のすべてのアクセスに対してキャッシュされた値を返す（それぞれのエントリが期限切れになっていない限り）、
キャッシュレイヤーによる高価なオペレーションの「ラッピング」です。

Akka HTTP には、Caffeine 上に構築された Cache API の 1 つの実装が含まれており、
時間ベースのエントリ満了をサポートする周波数ベースのキャッシュエヴィエーションセマンティクスを備えています。

## Dependency
Akka HTTP Cachingを使用するには、プロジェクトにモジュールを追加します。

```scala
val AkkaHttpVersion = "10.2.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion
```

## Basic design
キャッシュAPIの中心的なアイデアは、タイプTの実際の値そのものをキャッシュに保存するのではなく、
対応するフューチャー、すなわち**Future[T]タイプのインスタンスを保存すること**である。

このアプローチには、特定のキャッシュキー（リソースURIなど）への多くのリクエストが、
最初のリクエストが完了する前に到着してしまうという、雷のような群れの問題を解決するという利点があります。

通常（いわゆる「カウボーイ」エントリーのような特別なガード技術がない場合）は、
同じ結果を計算しようとする多くのリクエストがシステムリソースを奪い合うことになり、
システム全体のパフォーマンスが大幅に低下します。
Akka HTTP キャッシュを使用すると、 特定のキャッシュキーに対して到着した最初のリクエストにより、
後続のすべてのリクエストが「フックイン」する未来がキャッシュに置かれます。
最初のリクエストが完了すると同時に、他のすべてのリクエストも完了します。
これにより、すべてのリクエストの処理時間とサーバーの負荷を最小限に抑えることができます。

すべての Akka HTTP キャッシュの実装は Cache クラスに準拠しており、これによりキャッシュとのやり取りが可能になります。

キャッシュ API とともに、ルーティング DSL はルートでキャッシュを使用するためのいくつかのキャッシュディレクティブを提供しています。

## Frequency-biased LFU cache
周波数ベースのLFUキャッシュの実装では、保存できるエントリの最大数が定義されています。
最大容量に達した後、キャッシュは再び使用される可能性の低いエントリを退避させます。
たとえば、最近使用されていない、または頻繁に使用されていないエントリがキャッシュから削除されることがあります。

time-to-live および time-to-idle の有効期限が有限の期間に設定されている場合、時間ベースのエントリ有効期限が有効になります。
time-to-live と time-to-idle の両方の値が有限である場合、時間ベースのエントリ失効が有効になります。
両方の値が有限の場合、time-to-liveはtime-to-idle以上でなければならない。

注意
期限切れのエントリは、次のアクセスがあったときにのみ消去されます（または、容量制限によって捨てられます）。

単純なケースでは、application.conf ファイルの akka.http.caching の設定で容量と有効期限の設定を行い、
LfuCache.apply() を使用してキャッシュを作成します。
より高度な使い方をする場合は、ユースケースに特化した設定で LfuCache を作成することができます。

```scala
import akka.http.caching.scaladsl.Cache
import akka.http.caching.scaladsl.CachingSettings
import akka.http.caching.LfuCache
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.directives.CachingDirectives._
import scala.concurrent.duration._

// Use the request's URI as the cache's key
val keyerFunction: PartialFunction[RequestContext, Uri] = {
  case r: RequestContext => r.request.uri
}
val defaultCachingSettings = CachingSettings(system)
val lfuCacheSettings =
  defaultCachingSettings.lfuCacheSettings
    .withInitialCapacity(25)
    .withMaxCapacity(50)
    .withTimeToLive(20.seconds)
    .withTimeToIdle(10.seconds)
val cachingSettings =
  defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)
val lfuCache: Cache[Uri, RouteResult] = LfuCache(cachingSettings)

// Create the route
val route = cache(lfuCache, keyerFunction)(innerRoute)
```