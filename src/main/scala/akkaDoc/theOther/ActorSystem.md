# ActorSystem
アクターは、**状態と動作をカプセル化したオブジェクト**であり、
**受信者のメールボックスに格納されるメッセージを交換することによってのみ通信**します。

ある意味、アクターはオブジェクト指向プログラミングの最も厳格な形態ですが、 人と見なす方がよいでしょう。

アクターを使ってソリューションをモデル化する際には、人々のグループを想定し、
彼らにサブタスクを割り当て、彼らの機能を組織構造に配置し、 失敗をどのようにエスカレートさせるかを考えます
（すべて、実際には人々を扱わないという利点があるので、
彼らの感情的な状態や道徳的な問題を気にする必要はありません）。

このようにして得られた結果は、ソフトウェアの実装を構築するための精神的な足場となります。

## Hierarchical Structure (ヒエラルキー構造)
経済組織のように、アクターは自然に階層を形成します。
プログラムのある機能を監督するアクターは、そのタスクをより小さく、 
より管理しやすい部分に分割したいと思うかもしれません。
この目的のために、**子役**を立ち上げます。

アクターシステムの真骨頂は、**タスクを分割して委譲すること**で、
**1つのピースで処理できるほど小さくすること**にあります。
そうすることで、タスク自体が明確に構造化されるだけでなく、結果として生じるアクターは、
- どのメッセージを処理すべきか 
- どのように通常の反応をすべきか
- どのように失敗を処理すべきか
といった観点から推論することができます。

これは、失敗を外に漏らさないという目的で、
防御的なプログラミングに陥りやすいレイヤー型のソフトウェア設計と比較してみてください。
問題が適切な人に伝えられれば、すべてを「カーペットの下」に隠しておくよりも、
より良い解決策を見つけることができます。

さて、このようなシステムを設計する際に難しいのは、作業の構造をどう決めるかということです。
唯一のベストソリューションはありませんが、参考になるガイドラインはいくつかあります。

- あるアクターが非常に重要なデータを持っている場合（つまり、避けられるならばその状態を失ってはならない）、
このアクターは、危険性のあるサブタスクを子に分配し、これらの子の失敗を適切に処理すべきである。
リクエストの性質によっては、リクエストごとに新しいチャイルドを作成するのがベストかもしれません。
そうすることで、リプライを収集するための状態管理が簡単になります。
これはErlangの "Error Kernel Pattern "として知られています。

- もしあるアクタが自分の義務を遂行するために他のアクタに依存している場合、
そのアクタのlivenessを監視して、終了通知を受け取ったら行動するべきです。

- あるアクターが複数の責任を持っている場合、それぞれの責任を別の子に押し込むことで、
論理と状態をよりシンプルにすることができます。


## コンテナ構成
アクターの共同作業アンサンブルとしてのアクターシステムは、
スケジューリングサービス、設定、ロギングなどの共有設備を管理するための自然な単位です。
異なる構成を持つ複数のアクターシステムが同じJVM内に共存しても問題はありません。
Akka自体にはグローバルな共有ステートはありませんが、
最も一般的なシナリオでは、JVMごとに1つのアクターシステムが関与するだけです。

アクターシステムは、1つのノード内でも、ネットワーク接続を介しても、
アクターシステム間の透過的な通信が可能であり、分散アプリケーションの構築に最適です。

## Actor Best Practices
1. アクターは素敵な同僚のような存在であるべきです。
   無駄に他の人に迷惑をかけることなく効率的に仕事をこなし、リソースを占有しないようにします。
   プログラミングに置き換えると、これはイベントを処理し、
   イベントドリブンな方法でレスポンス（またはさらなるリクエスト）を生成することを意味します。
   アクターは、やむを得ない場合を除き、 外部のエンティティ（ロックやネットワークソケットなど）を
   ブロック（スレッドを占有して受動的に待つこと）してはいけません
   （後者の場合は「ブロックには注意深い管理が必要」を参照）。

2. ミュータブルなオブジェクトをアクター間で渡さない。
   それを確実にするためには、不変的なメッセージが望ましいです。
   ミュータブルな状態を外部に公開することでアクターのカプセル化が崩れると、
   通常のJavaの並行処理に戻ってしまい、あらゆる欠点が出てきます。

3. アクターはビヘイビアとステートのコンテナとして作られています。
   これを受け入れることは、ビヘイビアをメッセージ内で日常的に送信しないことを意味します（Scalaのクロージャを使うと誘惑されるかもしれませんが）。
   リスクの一つは、アクター間で変更可能な状態を誤って共有してしまうことです。
   このアクター・モデルの違反は、残念ながら、アクターでのプログラミングを素晴らしいものにしているすべての特性を壊してしまいます。

4. アクター システムのトップレベルのアクターは、エラー カーネルの最も内側に位置し、
   アプリケーションのさまざまなサブシステムの起動にのみ責任を負い、それ自体にはあまりロジックを含まない、
   真に階層的なシステムである必要があります。
   これには、障害処理に関する利点（構成の粒度とパフォーマンスの両方を考慮した場合）があり、
   また、過剰に使用された場合に単一の争点となるガーディアン・アクターの負担を軽減します。


## 気にしてはいけないこと
アクターシステムは、含まれるアクターを実行するために使用するように構成されたリソースを管理します。
1つのシステムには何百万ものアクターが存在するかもしれませんが、
アクターは豊富であると考えられており、1インスタンスあたり約300バイトのオーバーヘッドしかありません。
当然ながら、大規模なシステムでメッセージが処理される正確な順序は、
アプリケーションの作者がコントロールすることはできませんが、これも意図したものではありません。
Akka がフードの下で力仕事をしている間、一歩下がってリラックスしてください。

## アクターシステムの終了
アプリケーションがすべて完了したら、ユーザーガーディアンのアクターを停止させるか、
ActorSystemのterminateメソッドを呼び出すことができます。
これにより、CoordinatedShutdown が実行され、実行中のすべてのアクターが停止します。

ActorSystemの終了中に何らかの処理を実行したい場合は、CoordinatedShutdownを参照してください。
