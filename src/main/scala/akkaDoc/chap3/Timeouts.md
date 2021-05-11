# Timeouts
Akka HTTPには、悪意のある攻撃やプログラミングのミスからサーバーを保護するための、
さまざまな組み込みのタイムアウト機構が用意されています。

これらの中には、単なる構成オプション（コード内でオーバーライド可能）のものもあれば、
ストリーミングAPIに委ねられており、ユーザーコード内でパターンとして直接簡単に実装できるものもあります。

## idle timeouts
idle-timeoutは、指定された接続の最大非活動時間を設定するグローバル設定です。
つまり、コネクションが開いている状態で、アイドルタイムアウト時間を超えてリクエストやレスポンスが書き込まれなかった場合、
そのコネクションは自動的にクローズされます。

この設定は、サーバーサイド、クライアントサイドを問わず、すべての接続に対して同じように機能し、
以下のキーを使ってそれぞれ独立して設定することができます。

    akka.http.server.idle-timeout
    akka.http.client.idle-timeout
    akka.http.host-connection-pool.idle-timeout
    akka.http.host-connection-pool.client.idle-timeout

## Server timeouts
### Request timeouts
リクエストのタイムアウトは、ルートからHttpResponseを生成するのにかかる最大時間を制限する仕組みです。
タイムアウトが発生すると、サーバーは自動的にService Unavailable HTTPレスポンスを送信し、
コネクションを閉じて、いつまでもリークしないようにします。

```scala
HttpResponse(StatusCodes.ServiceUnavailable, entity = "The server was not able " +
  "to produce a timely response to your request.\r\nPlease try again in a short while!")
```

デフォルトのリクエストタイムアウトは、すべてのルートにグローバルに適用され、
akka.http.server.request-timeout設定（デフォルトは20秒）を使用して設定できます。

リクエストタイムアウトは、TimeoutDirectiveのいずれかを使用して、指定されたルートのランタイムに設定できます。

### Bind timeout
バインドタイムアウトは、
(Http().bind* メソッドのいずれかを使用して) TCP バインド処理を完了しなければならない時間です。
これは、akka.http.server.bind-timeout 設定を使用して設定できます。

### Linger timeout
リンガータイムアウトは、すべてのデータがネットワーク層に配信された後に、HTTPサーバーの実装が接続を維持する期間です。
この設定は、SO_LINGER ソケットオプションに似ていますが、OS レベルのソケットだけでなく、
Akka IO / Akka Streams ネットワークスタックも対象となります。
この設定は、サーバー側ですでに完了したとみなされている接続を
クライアントが開いたままにしないようにするための追加の予防措置です。

ネットワークレベルのバッファ（Akka Stream / Akka IOネットワークスタックのバッファを含む）に、
サーバー側がこの接続を完了したとみなす所定の時間内に、クライアントに転送できるデータ量を超えるデータが含まれている場合、
クライアントは接続のリセットに遭遇する可能性があります。

infinite に設定すると、自動的な接続の終了が無効になります（接続が漏れる危険性があります）。