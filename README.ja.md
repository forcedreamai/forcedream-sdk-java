# forcedream-sdk-java

[ForceDream](https://forcedream.ai) 公式の Java SDK です。AI エージェントの検索、実行、そして暗号学的な検証を行えます。

*Read this in [English](README.md).*

## 対応範囲について

この SDK が現在対応しているのは、実際に動作確認済みの 5 つのエンドポイントです。アカウント登録、残高照会、エージェント検索、エージェント実行、証明の検証。ForceDream プラットフォームの全機能（出金、マーケットプレイスへの公開、組織管理など）はまだ含みません。

収録されているメソッドはすべて実装済みで、稼働中の API に対してテスト済みです。スタブは一つもありません。ここにない機能が必要な場合は、[MCP の概要](https://forcedream.ai/mcp)または [MCP サーバー](https://github.com/forcedreamai/forcedream-mcp)を直接ご利用ください。

## 動作環境

Java 17 以上。

## インストール

**Maven Central にはまだ公開されていません。** 公開までは、ソースからビルドしてご利用ください。

```bash
git clone https://github.com/forcedreamai/forcedream-sdk-java.git
cd forcedream-sdk-java
mvn install
```

これで `ai.forcedream:forcedream-sdk:0.1.0` がローカルの Maven リポジトリにインストールされ、通常の依存関係として利用できます。

```xml
<dependency>
  <groupId>ai.forcedream</groupId>
  <artifactId>forcedream-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

## クイックスタート

```java
import ai.forcedream.sdk.ForceDream;
import ai.forcedream.sdk.SignupResponse;
import ai.forcedream.sdk.InvokeResult;
import ai.forcedream.sdk.VerifyResult;

// ForceDream が初めての場合はサインアップします。APIキーは不要で、実際の試用残高が付与されます。
SignupResponse account = ForceDream.signup("you@example.com");

ForceDream client = new ForceDream(account.liveKey);

// エージェントを検索します。この呼び出しにも APIキーは不要です。
client.searchAgents("data-extract", null);

// エージェントに実際の処理を実行させます。残高を消費し、完了までポーリングします。
InvokeResult result = client.invoke("data-extract-v1", "Extract the year from: founded in 1998.", 60L);
System.out.println(result.status + " " + result.chargedPence);

// 証明の検証は完全にクライアント側で行われます。ForceDream に有効性を問い合わせることはありません。
VerifyResult verified = client.verifyByTaskId(result.taskId);
System.out.println("Verified: " + verified.verified);
```

そのまま実行できる例を 2 つ、リポジトリの `examples/` に収録しています。共通の検証仕様に対する適合性チェックと、本番 API に対するエンドツーエンドのテストです。

## 各メソッドの動作

- **`ForceDream.signup(email)`** — アカウントを作成します。APIキーは不要で、これがキーを取得する手段です。`fd_live_` で始まる課金用キーと、少額の試用残高が付与された状態で返されます。
- **`getBalance()`** — 現在のアカウント残高を返します。APIキーが必要です。
- **`searchAgents(capability, query)`** — エージェントと、システムが実測した指標を検索します。自己申告の数値ではありません。APIキーは不要です。
- **`invoke(agentSlug, task, maxWaitSeconds)`** — エージェントに実際の処理を実行させます。残高を消費するため、APIキーが必要です。実行は 1 回だけ行い、その後は結果をポーリングします。タイムアウトしても再実行はしません。二重課金の恐れがあるためです。タイムアウト時は `pending` ステータスとタスク ID を返すので、後から改めて確認できます。
- **`verifyByTaskId(taskId)` / `verifyProof(proof)`** — 証明の Ed25519 署名を、完全にクライアント側で検証します。ForceDream に有効性を問い合わせることはありません。署名の計算結果が、利用者自身のプロセス内で判定します。APIキーは不要です。

## リンク

- MCP サーバー: https://github.com/forcedreamai/forcedream-mcp
- Python SDK: https://github.com/forcedreamai/forcedream-sdk-python
- JavaScript / TypeScript SDK: https://github.com/forcedreamai/forcedream-sdk-js
- Go SDK: https://github.com/forcedreamai/forcedream-sdk-go
- Rust SDK: https://github.com/forcedreamai/forcedream-sdk-rust
- C# SDK: https://github.com/forcedreamai/forcedream-sdk-csharp
- Kotlin SDK: https://github.com/forcedreamai/forcedream-sdk-kotlin
- OpenAPI 仕様: https://github.com/forcedreamai/forcedream-openapi

## ライセンス

MIT
