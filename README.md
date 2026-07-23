# MokeHud Android

デバッグ時に、動作中アプリの画面上へ Analytics イベントなどをオーバーレイ表示する HUD ライブラリです。

`:hud` を依存に追加するだけで HUD が有効になります(`Application` サブクラス不要 —
マニフェスト宣言の `ContentProvider` が自動で初期化します)。

## インストール

`:hud` は GitHub Pages 上の Maven リポジトリ（認証不要）で配布しています。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mokelab.github.io/moke-hud-android/") }
    }
}
```

```kotlin
// アプリモジュールの build.gradle.kts
dependencies {
    implementation("com.mokelab.hud:hud-android:0.1.0")
}
```

## リリース手順（メンテナ向け）

`gh-pages` ブランチを Maven リポジトリとして配信しています。ローカルの git worktree
`.gh-pages/`（`.gitignore` 済み）へ成果物を出力し、commit → push します。

初回のみ:

```bash
git worktree add --orphan -b gh-pages .gh-pages
(cd .gh-pages && touch .nojekyll && git add .nojekyll \
  && git commit -m "chore: init gh-pages maven repo" && git push -u origin gh-pages)
```

その後、GitHub の Settings → Pages で **Source = `gh-pages` / `/ (root)`** を設定します。

各リリース:

```bash
# 1. hud/build.gradle.kts の version を更新
git -C .gh-pages pull
./gradlew :hud:publish            # .gh-pages/ に Maven レイアウトを出力
git -C .gh-pages add -A
git -C .gh-pages commit -m "publish com.mokelab.hud:hud-android:<version>"
git -C .gh-pages push
```

## モジュール

- **`:hud`** — ライブラリ本体（`com.mokelab.hud.android`）。配布対象。
- **`:demo`** — 動作確認用のサンプルアプリ。`implementation(project(":hud"))` で参照。

開発時のビルド・テストコマンドは [`CLAUDE.md`](CLAUDE.md) を参照してください。
