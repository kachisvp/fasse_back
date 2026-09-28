<details>

<summary>よく使うコマンド</summary>

# MySQL

## Mac

```
mysql --version
mysql.server start
mysql.server restart
mysql.server stop
mysql -u root -p -e "select version();" 2>&1
```

## Windows

VSCodeを管理者権限で起動すること

```
mysql --version
net start MySQL84
net stop MySQL84
mysql -u root -p -e "select version();" 2>&1
```

# VSCode

```
# キャッシュを使用せず最新の情報に更新
./gradlew --refresh-dependencies
# ビルド(テスト・カバレッジ 80% の検証を含む)
./gradlew build
# テスト(fasse_test データベースを使用する)
./gradlew test
# カバレッジレポート(build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestReport
# 開発用 DB(fasse)の全データを削除してテストデータを投入
./gradlew seedLocal -Pconfirm=yes
# ローカル起動(システム環境変数 SPRING_PROFILES_ACTIVE=local が必要)
./gradlew bootRun
```

**テストは[fasse_test]のデータを削除・投入する。データベース名が[_test]で終わらない場合、テストデータの投入は中止される**

[Ctrl + Shift + P]を押下し、コマンドパレットを開く

```
> Developer: Reload Window
```

</details>

<details>

<summary>Windows環境構築</summary>

# Windows 環境構築

## Google Chrome

Google Chrome がインストールされていないと、[flutter doctor -v]が終了しないため、インストールする

## Git For Windows

### install

[Override the default branch name for new repositories]を選択 > [main]に変更
[Checkout as-is, commit as-is]を選択
他は default で[Next] > [Finish]を押下

### コマンドプロンプトを開き、version を確認

git --version

### Git 初期設定

git config --global user.name "Namae Myoji"
git config --global user.email "_username_@example.com"

## TortoiseGit

### install

すべてデフォルトでインストール

## Flutter SDK

### install

[C:\Users\'_username_'\dev\flutter\]となる様に保存

### システム環境変数に以下を追加

PATH=%PATH%;"C:\Users\'_username_'\dev\flutter\bin"

### Flutter が利用可能になっていることを確認

コマンドプロンプトを開き、以下のコマンドを実行

```
flutter --version
flutter doctor -v
```

**10 分程度、何も表示されずに処理に時間が掛かる可能性あり**

## OpenJDK21

[openjdk-21+35_windows-x64_bin.zip]
[C:\Users\'_username_'\dev\jdk-21\]となる様に保存

### システム環境変数に以下を追加

JAVA*HOME="C:\Users\'\_username*'\dev\jdk-21"
PATH=%PATH%;"%JAVA_HOME%\bin"

### コマンドプロンプトを開き、version を確認

java --version

## Visual Studio Code

### install

すべてデフォルトでインストール

### システム環境変数に以下を追加

SPRING_PROFILES_ACTIVE=local

### Visual Studio Code Settings

[File] > [Preferences] > [Settings]を押下 > 右上の[Open Settings(JSON)]を押下
以下の設定を追加

```
{
    "java.jdt.ls.java.home": "C:/Users/_username_/dev/jdk-21",
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-21",
            "path": "C:/Users/_username_/dev/jdk-21",
            "default": true,
        },
    ],
    "java.import.gradle.java.home": "C:/Users/_username_/dev/jdk-21",
    // "http.proxy": "http://_domain_:8080",
    // "https.proxy": "http://_domain_:8080",
    // "http.proxyStrictSSL": false
}
```

### VSCode Extensions

[Visual Studio Code] > [左側の Extensions]を押下
[Search Extensions in Marketplace]で以下を検索して[install]を押下

- Flutter
- Extension Pack for Java
- Gradle for Java
- Spring Boot Extension Pack

## MySQL 8.4 LTS

### MySQL 8.0 uninstall

MySQL 8.0がインストールされている場合、以下の手順でuninstallする

#### サービス停止

コマンドプロンプトを管理者権限で起動し、以下のコマンドを実行

```
net stop MySQL80
```

> [MySQL80 サービスは正常に停止されました。]が表示されることを確認

#### uninstall

[コントロールパネル]を開き、以下の「MySQL」とつくものをすべてアンインストール

- MySQL Server 8.0
- MySQL Installer - Community
- MySQL Shell、MySQL Workbench、MySQL Router、Connector 類（入っていれば）

#### フォルダ削除

以下のフォルダを削除

- C:\ProgramData\MySQL

#### サービスが消えたかを確認

[Win + R] > [services.msc]を入力 > [OK]を押下

> [MySQL80]が残っている場合、以下を実行

コマンドプロンプトを管理者権限で起動し、以下のコマンドを実行

```
sc delete MySQL80
```

[サービス]にて、左上の[最新の情報に更新]を押下

> サービスから[MySQL80]が削除されたことを確認

#### 環境変数 PATH から削除

[Win + R] > [sysdm.cpl]を入力 > [OK]を押下
[詳細設定] > [環境変数]を押下
[システム環境変数] > [Path]を選択 > [編集]を押下
[C:\Program Files\MySQL\MySQL Server 8.0\bin]を選択 > [削除]を押下

PCを再起動

### download

[https://dev.mysql.com/downloads/mysql/]を開く
[Select Version]: [8.4.x LTS]を選択
[Select Operating System]: [Microsoft Windows]を選択
[Windows (x86, 64-bit), MSI Installer]の[Download]を押下
[No thanks, just start my download.]を押下して[mysql-8.4.x-winx64.msi]を保存

**[VC_redist.x64.exe](Microsoft Visual C++ 再頒布可能パッケージ)が未インストールの環境では、先にインストールが必要**

### install

[mysql-8.4.x-winx64.msi]を実行
ライセンスに同意して[Next]を押下
[Choose Setup Type]: [Typical]を選択、その他すべてデフォルトでインストール
インストール完了画面で[Run MySQL Configurator]にチェックが入っていることを確認して[Finish]を押下

### MySQL Configurator で初期設定

[MySQL Configurator]が起動したら、以下の通り設定する

- [Welcome]: [Next]
- [Data Directory]: [C:\ProgramData\MySQL\MySQL Server 8.4\]であることを確認 > [Next]
- [Type and Networking]: [Config Type]は[Development Computer]、[Port]は[3306]のまま[Next]
- [Accounts and Roles]: [MySQL Root Password], [Repeat Password]: [_任意のパスワード_]を入力して[Next]
- [Windows Service]: [Windows Service Name]が[MySQL84]であることを確認、その他デフォルトで[Next]
- [Server File Permissions]: [Next]
- [Sample Databases]: チェックを入れずに[Next]
- [Apply Configuration]で[Execute]を押下
- すべての項目にチェックが付き、[Next] > [Finish]

> [The configuration for MySQL Server 8.4.x was successful.]と表示されたことを確認

**[MySQL Configurator]は後から再実行可能
[スタートメニュー] > [MySQL] > [MySQL Configurator 8.4]**

### システム環境変数に以下を追加

> [PATH=%PATH%;"C:\Program Files\MySQL\MySQL Server 8.4\bin"]となる様に追加する

[Win + R] > [sysdm.cpl]を入力 > [OK]を押下
[詳細設定] > [環境変数]を押下
[システム環境変数] > [Path]を選択 > [編集]を押下
[新規]を押下 > [C:\Program Files\MySQL\MySQL Server 8.4\bin]を入力 > [削除]を押下
[%JAVA_HOME%\bin]の下に配置されるまで[上へ]を押下
[環境変数名]: [OK]を押下
[環境変数]: [OK]を押下
[システムのプロパティ]: [OK]を押下

### コマンドプロンプトを開き、version を確認

mysql --version

> [mysql Ver 8.4.x for Win64 on x86_64 (MySQL Community Server - GPL)]が表示されることを確認

### 引き続きコマンドプロンプトで database を作成

コマンドプロンプトを開き、以下のコマンドを実行

```
mysql -u root -p
```

install 時の[_任意のパスワード_]を入力
以下を入力

```
create user admin identified by '_任意のパスワード_';
create database fasse;
create database fasse_test;
grant all on fasse.* to admin;
grant all on fasse_test.* to admin;
quit
```

- [fasse]: ローカル起動(local プロファイル)で使うデータベース
- [fasse_test]: 自動テスト(test プロファイル)で使うデータベース。テストのたびにデータを削除・投入するため、[fasse]とは分ける
- テーブルはアプリケーション起動時・テスト開始時に Flyway が作成するため、ここではデータベースの作成と権限付与のみを行う
- Flyway がテーブルを作成するため、[admin]には両データベースへの全権限を付与する

**MySQL 8.4 では[mysql_native_password]認証が既定で無効のため、ユーザーは[caching_sha2_password]で作成される。
古いドライバ/ツールで接続できない場合は、ドライバを最新版に更新すること**

### VSCode Extensions

[Visual Studio Code] > [左側の Extensions]を押下
[Search Extensions in Marketplace]で以下を検索して[install]を押下

- MySQL Shell for VS Code

左の[MySQL Shell for VS Code]を押下
[DB Connection Overview]を押下
[New Connection]を押下
以下を入力して[OK]を押下

```
Caption: fasse
Username: admin
```

左の[DATABASE CONNECTION] > [fasse]を右クリック > [Open New Database Connection]を押下
install 時の[_任意のパスワード_]を入力

### [fasse]の[DB Notebook]が開いたらバージョンを確認

以下を入力し、[Ctrl + Enter]を押下

```
select version();
```

> [8.4.x]と表示されることを確認

## Visual Studio Code 動作確認手順

### MySQL

コマンドプロンプトを開き、以下のコマンドを実行

```
# 停止
mysqladmin -u root -p shutdown
# 起動
mysqld
```

上記コマンドで起動できない場合、[サービス]から起動する

```
[Win + R > services.msc]を入力
[MySQL84]を右クリック > [再起動]を押下
```

### [VC_redist.x64.exe]について

指示に従って[VC_redist.x64.exe]のインストールが必要な環境もある
Wizard に従って証明書をインストール
VSCode を再起動

### SpringBoot

[fasse_back]プロジェクトを[Git Clone]
[fasse_back]プロジェクトを[Visual Studio Code]で開く

#### application-\*.yaml 設定

[src/main/resources/application-*.yaml]は DB のパスワード等を直接記載するため、[.gitignore]の対象としてリポジトリに含めていない
以下の 2 ファイルを[src/main/resources/]に作成する

**作成したファイルはコミットしないこと**

[application-local.yaml]: ローカル起動用

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fasse?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
    username: admin
    password: _任意のパスワード_

fasse:
  jwt:
    # fasse_infra/jwt_public_key.pem の内容を記載する
    public-key-pem: |
      -----BEGIN PUBLIC KEY-----
      _fasse_infra/jwt_public_key.pem の内容_
      -----END PUBLIC KEY-----
  cors:
    allowed-origins: http://localhost:5000
```

[application-test.yaml]: 自動テスト用

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fasse_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
    username: admin
    password: _任意のパスワード_

fasse:
  jwt:
    # テストではテストコード内で生成した鍵ペアの公開鍵を設定するため空とする
    public-key-pem: ""
  cors:
    allowed-origins: http://localhost:5000
```

- [_任意のパスワード_]は database 作成時に[admin]に設定したパスワード
- [public-key-pem]が未設定・不正の場合もアプリケーションは起動するが、全 API が 401 を返す

[src/main/java/com/example/fasse_back/FasseBackApplication.java]をデバッグ実行

- 起動時に Flyway が[fasse]にテーブルを作成する(作成済みの場合は未適用のマイグレーションのみ適用する)
- ログはコンソールと[logs/fasse_back.log]に出力される(日次ローテーション、7 日保持)

#### 開発用データの投入(任意)

[fasse]はテーブルのみの空の状態で始まるため、動作確認用にテストデータ([src/test/resources/testdata/])を投入できる

```
./gradlew seedLocal -Pconfirm=yes
```

**実行すると[fasse]の全データを削除してから投入する。登録済みのデータはすべて消えるため注意すること**

- [-Pconfirm=yes]を付けない場合は、DB に接続せずに終了する
- 接続先は[application-local.yaml]の設定。ホストが[localhost]以外の場合は実行されない
- テーブルが未作成でも実行できる(投入前に Flyway がテーブルを作成する)
- 投入後にアプリケーションから登録しても、伝票番号・id はテストデータと重複しない
- 動作確認でデータが崩れた場合も、同じコマンドで元の状態に戻せる

#### JWT の取得と API の呼び出し

本リポジトリは JWT を発行しないため、[fasse_infra]の stg 環境の JWT 発行 API で取得する(有効期限 30 日)

- [POST /auth/token](AccessKey 経路)を呼び出して JWT を取得する。リクエストの形式は[fasse_infra]の[docs/specs/authentication/]を参照
- API の呼び出し時は[Authorization: Bearer <JWT>]ヘッダを付ける
- Postman で確認する場合は[fasse_infra/postman/]のコレクションを使い、ベース URL を[http://localhost:8080]に変更した環境で実行する

```
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/items
```

> JWT が無い・不正な場合は 401 と[{"message": "...", "requestId": "..."}]が返る

### Flutter

[fasse_front]プロジェクトを[Git Clone]
[fasse_front]プロジェクトを[Visual Studio Code]で開く
[Ctrl + @]を押下して[Terminal]を開く
以下のコマンドを実行する

```
flutter clean
flutter pub get
flutter build web
flutter run -d chrome
```

#### CORS 対応

Flutter-SpringBoot をローカル環境で連携すると、[CORS: Cross-Origin Resource Sharing]で止められるため、開発用に以下を修正

[C:\Users\_username_\dev\flutter\packages\flutter_tools\lib\src\web\chrome.dart]を開く

```
      '--disable-extensions',
      '--disable-web-security', // 開発用にこの行を追加
```

[C:\Users\_username_\dev\flutter\bin\cache\flutter_tools.stamp]を削除
**ビルド時に再作成されるファイルのため、削除しても問題ない**

Chrome で Flutter アプリが動作することを確認

### [MySQL Shell for VS Code]の証明書削除手順

Chrome > [設定] > [プライバシーとセキュリティ] > [セキュリティ] > [証明書の管理]を押下
[ローカル証明書] > [Windows からインポートした証明書を管理する]を押下
[信頼されたルート証明機関] > [発行先: MySQL Shell Auto Generated CA Certificate]を選択 > [削除]を押下
警告されるが、これで削除できる。
再度、[Run Welcome Wizard]を実行すれば、再インストールされる。

### Android Studio をインストールした場合

flutter doctor --android-licenses

</details>

<details>

<summary>実装機能</summary>

# システム構成

- Database: MySQL
- Back-End: SpringBoot
- Front-End: Flutter

# 実装機能

- データ抽出、表示
- データ登録
- 画像登録
- ファイルアップロード、データ登録
- ファイルダウンロード
- PDF 出力
- ログイン
- ログアウト
- ログ出力
- オンデマンドバッチ

# テスト自動化

- SpringBoot のテスト自動化
- Flutter のテスト自動化

# 教育目標

- SpringBoot で MySQL からデータを抽出し、JSON データを返却できること
- SpringBoot で JSON データを MySQL に登録できること
- Flutter で WebAPI の GET メソッドをコールし、返却された JSON データを表示できること
- Flutter で WebAPI の POST メソッドをコールし、JSON データを送信できること

</details>
