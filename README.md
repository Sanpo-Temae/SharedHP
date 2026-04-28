# SharedHP

[English README](README_en.md)

SharedHP は、登録したプレイヤーたちが 1つのHPを共有する **Minecraft Java版 Paper専用** プラグインです。

協力サバイバル、企画動画、イベントサーバーなどで「誰かがダメージを受けると全員の共有HPが減る」遊びを作るためのプラグインです。

## 重要: Paper専用

SharedHP は `paper-api:1.21.11-R0.1-SNAPSHOT` を使ってビルドしています。

- 対応: **Paper 1.21.11**
- 必要Java: **Java 21**
- 非対応: **Spigot / Bukkit / CraftBukkit**
- SpigotMC掲載時の注意: **Requires Paper. Spigot/Bukkit are not supported.**

Paper以外のサーバーでは、読み込みに失敗したり、正しく動作しない可能性があります。

## 主な機能

- `/sharedhp add <player>` で任意人数の参加者を登録
- 登録済みのオンライン参加者が 1つの内部HPを共有
- ダメージは共有HPから1回分だけ減少
- 回復量はデフォルトで通常の25%
- `/sharedhp heal <percent>` で回復効率をゲーム内から変更可能
- 参加者のHPを共有HPに同期
- 参加者の吸収ハートを無効化
- ボスバーで共有HPを表示
- 共有HPが0になるとオンライン参加者全員が死亡
- 参加者ごとの被ダメージランキングを表示
- 参加者リストは `plugins/SharedHP/config.yml` に保存

## ダウンロード

GitHub Releases または SpigotMC のリソースページから、最新版の `SharedHP-x.y.z.jar` をダウンロードしてください。

サーバーの `plugins` フォルダに入れる SharedHP のJarは **必ず1つだけ** にしてください。

悪い例:

```text
plugins/
  SharedHP-0.1.3.jar
  SharedHP-0.1.4.jar
```

良い例:

```text
plugins/
  SharedHP-1.0.0.jar
```

## 導入方法

1. サーバーを停止します。
2. 最新版の `SharedHP-x.y.z.jar` をサーバーの `plugins` フォルダに入れます。
3. サーバーを起動します。
4. `/plugins` を実行し、`SharedHP` が緑色で表示されることを確認します。
5. `/sharedhp status` で状態を確認します。
6. `/sharedhp add <player>` で参加者を登録します。
7. `/sharedhp start` で共有HPを開始します。

更新時も `/reload` は使わず、`stop -> Jar入れ替え -> 起動` の流れを推奨します。

## コマンド

| コマンド | 権限 | 説明 |
| --- | --- | --- |
| `/sharedhp status` | `sharedhp.view` | active状態、共有HP、登録人数、オンライン対象者、ボスバー状態、回復倍率を表示します。 |
| `/sharedhp list` | `sharedhp.view` | 登録済み参加者とオンライン/オフライン状態を表示します。 |
| `/sharedhp damage` | `sharedhp.view` | 被ダメージランキングをオンライン参加者に表示します。 |
| `/sharedhp add <player>` | `sharedhp.admin` | オンライン中のプレイヤーを参加者に追加します。 |
| `/sharedhp remove <player>` | `sharedhp.admin` | 参加者を削除します。 |
| `/sharedhp start` | `sharedhp.admin` | 登録済み参加者が1人以上オンラインなら共有HPを開始します。 |
| `/sharedhp start force` | `sharedhp.admin` | 互換用の別名です。現在は `/sharedhp start` と同じ条件で開始します。 |
| `/sharedhp stop` | `sharedhp.admin` | 共有HP処理を停止し、ボスバーを非表示にします。 |
| `/sharedhp reset` | `sharedhp.admin` | 共有HPと被ダメージランキングをリセットします。 |
| `/sharedhp set <value>` | `sharedhp.admin` | 共有HPを指定値に設定します。`0.0` から最大HPの範囲に丸められます。 |
| `/sharedhp heal <percent>` | `sharedhp.admin` | 回復効率を変更します。例: `/sharedhp heal 25` で25%。 |
| `/sharedhp damage reset` | `sharedhp.admin` | 被ダメージランキングをリセットします。 |

短縮コマンド:

```text
/shp
```

## 権限

| 権限 | デフォルト | 説明 |
| --- | --- | --- |
| `sharedhp.view` | `op` | status、list、damage などの閲覧系コマンドを許可します。 |
| `sharedhp.admin` | `op` | すべての管理コマンドを許可します。`sharedhp.view` を含みます。 |

コンソールからはすべてのコマンドを実行できます。

## 設定ファイル

`config.yml` は初回起動時に自動生成されます。

```text
plugins/SharedHP/config.yml
```

デフォルト設定:

```yaml
participants: []

max-shared-health: 20.0
heal-multiplier: 0.25
bossbar-title-format: "共有HP: %.1f / %.1f | 回復効率%.0f%%"
```

### `participants`

登録済み参加者のUUIDリストです。

基本的には以下のコマンドで管理してください。

```text
/sharedhp add <player>
/sharedhp remove <player>
```

サーバー停止中であれば、手動編集も可能です。

### `max-shared-health`

共有HPの最大値です。

MinecraftのHP値:

- `20.0` = 10ハート
- `2.0` = 1ハート
- `1.0` = 半ハート

### `heal-multiplier`

通常の回復量のうち、共有HPへ反映する割合です。

デフォルト:

```yaml
heal-multiplier: 0.25
```

これは回復効率25%を意味します。

ゲーム内から変更する例:

```text
/sharedhp heal 25
/sharedhp heal 50
/sharedhp heal 100
```

コマンドではパーセント数値で指定します。`/sharedhp heal 25` は `heal-multiplier: 0.25` として保存されます。

### `bossbar-title-format`

ボスバーのタイトル形式です。

最初の `%.1f` は現在の共有HP、2つ目の `%.1f` は最大共有HPです。
`%.0f` は現在の回復効率パーセントです。

文字として `%` を表示したい場合は `%%` と書いてください。

## よくある質問

### Spigotに対応していますか？

いいえ。SharedHP は Paper専用プラグインです。Spigot、Bukkit、CraftBukkit は非対応です。

### ゲーム内でコマンドが赤く表示されます。

以下を確認してください。

- `/plugins` で SharedHP が緑色表示されているか
- OP権限、または必要な権限を持っているか
- SharedHP のJarを複数入れていないか
- サーバーが Paper 1.21.11 か

### 更新後に `/reload` を使っていいですか？

推奨しません。サーバーを停止し、Jarを入れ替えてから起動してください。

### 回復量が少なく感じます。

デフォルトでは回復量が `0.25` 倍、つまり25%になります。`/sharedhp heal <percent>` または `heal-multiplier` で変更できます。

### エラーはどこで確認できますか？

以下のログを確認してください。

```text
logs/latest.log
```

不具合報告時は、該当するログ部分と Paper バージョンを共有してください。

## トラブルシューティング

- `/plugins` に SharedHP が出ない場合は、Jarが `plugins` フォルダ直下にあるか確認してください。
- `/plugins` で SharedHP が赤色の場合は、`logs/latest.log` を確認してください。
- `UnsupportedClassVersionError` が出た場合は、Java 21 でサーバーを起動してください。
- コマンドが出ない場合は、OP権限または権限設定を確認してください。
- config変更が反映されない場合は、サーバー停止中に編集してから起動してください。

## 不具合報告

不具合は GitHub Issues から報告できます。

報告時は、可能な範囲で以下を含めてください。

- SharedHP のバージョン
- Paper のバージョン
- Java のバージョン
- `plugins/SharedHP/config.yml`
- `logs/latest.log` の該当部分
- 再現手順

## リンク

- GitHub Releases: このリポジトリの Releases ページを確認してください。
- SpigotMC Resource: 準備中
- ブログ解説記事: 準備中
- YouTube解説動画: 準備中

## 更新履歴

[CHANGELOG.md](CHANGELOG.md) を確認してください。

## ライセンス

SharedHP は MIT License で公開されています。詳しくは [LICENSE](LICENSE) を確認してください。
