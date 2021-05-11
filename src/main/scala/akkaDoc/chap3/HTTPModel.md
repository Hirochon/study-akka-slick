# HTTP Model
Akka HTTP モデルには、HTTP リクエスト、レスポンス、共通ヘッダーなど、
すべての主要な HTTP データ構造の、深く構造化された、完全に不変の、ケースクラスベースのモデルが含まれています。
このモデルは akka-http-core モジュールに含まれており、Akka HTTP のほとんどの API の基礎となっています。

akka-http-core は中心となる HTTP データ構造を提供しているため、
コードベースの多くの場所で以下のインポートを見つけることができるでしょう (おそらくあなた自身のコードにも同様に)。

```scala
import akka.http.scaladsl.model._
```

これにより、主に関連性の高いタイプがすべて対象となります。

- 中心的なメッセージ・モデルであるHttpRequestとHttpResponse。
- headers：定義済みのすべてのHTTPヘッダ・モデルとサポート・タイプを含むパッケージ。
- Uri、HttpMethods、MediaTypes、StatusCodesなどのサポート・タイプです。

共通のパターンは、あるエンティティのモデルが不変の型 (クラスまたは trait) で表され、
HTTP 仕様で定義されたエンティティの実際のインスタンスは、
型の名前に末尾に複数の「s」を付けた付随するオブジェクトに格納されるというものです。

For example:

- Defined HttpMethod instances live in the HttpMethods object.
- Defined HttpCharset instances live in the HttpCharsets object.
- Defined HttpEncoding instances live in the HttpEncodings object.
- Defined HttpProtocol instances live in the HttpProtocols object.
- Defined MediaType instances live in the MediaTypes object.
- Defined StatusCode instances live in the StatusCodes object.

