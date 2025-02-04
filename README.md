<details>

<summary>Mac環境構築</summary>


# Mac環境構築
## Google Chrome
Google Chromeがインストールされていないと、[flutter doctor -v]が終了しないため、インストールする


## Git
### install
brew install git


### Terminalを開き、versionを確認
git --version


### Git初期設定
git config --global user.name "Namae Myoji"
git config --global user.email "_username_@example.com"


## SourceTree
### install
公式サイトからダウンロード、インストール


### Git Credential Manager
[Git Credential Manager]をinstallしないと、pushするのにtokenが必要になる
[Git for Windows]の場合、[Git]のinstall時に一緒にinstallされる
```
brew install --cask git-credential-manager
```


## Flutter SDK
### install
[/Users/_username_/dev/flutter]となる様に保存


### PATHに追加
```
vi ~/.zshrc
export PATH=${HOME}/dev/flutter/bin:${PATH}
source ~/.zshrc
```


### Flutterが利用可能になったことを確認
Terminalを開き、以下のコマンドを実行
```
flutter --version
flutter doctor -v
```



## OpenJDK
### install
brew search openjdk
brew install openjdk@21


### PATHに追加
```
vi ~/.zshrc
export PATH=/usr/local/opt/openjdk@21/bin:${PATH}
source ~/.zshrc
```


### Terminalを開き、versionを確認
java --version



## Visual Studio Code
### install
すべてデフォルトでインストール


### システム環境変数に以下を追加
```
vi ~/.zshrc
export SPRING_PROFILES_ACTIVE_local
source ~/.zshrc
```


### Visual Studio Code Settings
~~Java設定の必要があるかを確認する~~


### Extensions
以下を検索して[install]を押下
- Flutter
- Extension Pack for Java
- Gradle for Java
- Spring Boot Extension Pack



## MySQL
### install
defaulではMySQL9.2がinstallされてしまうため、versionを指定
```
brew search mysql

brew install mysql@8.4
brew info mysql
```

### PATHに追加
```
vi ~/.zshrc
export PATH=/usr/local/opt/mysql@8.4/bin:${PATH}
source ~/.zshrc
```

### Terminalを開き、versionを確認
mysql --version


### command
mysql.server start
mysql.server restart
mysql.server stop


### databaseを作成
初回はパスワードなしでログイン
```
mysql -uroot
set password for root@localhost='_任意のパスワード_';
quit
```

2回目以降は[_任意のパスワード_]を入力
```
mysql -u root -p
create user admin identified by '_任意のパスワード_';
create database fasse;
grant all on fasse.* to admin;
grant select, insert on fasse.* to admin;
quit
```


### VSCode Extensions
以下を検索して[install]を押下
- MySQL Shell for VS Code

左の[MySQL Shell for VS Code]を押下
[New Connection]を押下
以下を入力して[OK]を押下
```
Caption: fasse
Username: admin
```
左の[DATABASE CONNECTION] > [fasse]を右クリック > [Open New Database Connection]を押下
install時の[_任意のパスワード_]を入力


### [fasse]の[DB Notebook]が開いたらバージョンを確認
以下を入力し、[Cmd + Enter]を押下
```
select version();
```


### 動作確認用のschema, dataを投入
以下を入力し、[Cmd + Enter]を押下
```
use fasse
[./src/test/resources/schema.sql]を開く > 全選択 > 貼り付け > [Cmd + Enter]を押下
[./src/test/resources/data.sql]を開く > 全選択 > 貼り付け > [Cmd + Enter]を押下
```



# Visual Studio Code 動作確認手順
## SpringBoot
[fasse_back]プロジェクトを[Git Clone]
[fasse_back]プロジェクトを[Visual Studio Code]で開く


### application.yaml設定
[src/main/resources/application.yaml]をコピーして[src/main/resources/application-local.yaml]を作成
以下を修正
```
_dbname_: fasse
_username_: admin
_password_: [_任意のパスワード_]
```


### gradlewに実行権限を付与
```
chmod +x ./gradlew
```
[src/main/java/com/example/fasse_back/FasseBackApplication.java]をデバッグ実行
[http://localhost:8080/users]にアクセスし、[m_user]からJSONデータを取得することを確認



## Flutter
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


### CORS対応
Flutter-SpringBootをローカル環境で連携すると、[CORS: Cross-Origin Resource Sharing]で止められるため、開発用に以下を修正

[~/dev/flutter/packages/flutter_tools/lib/src/web/chrome.dart]を開く
```
      '--disable-extensions',
      '--disable-web-security', // 開発用にこの行を追加
```

[~/dev/flutter/bin/cacheflutter_tools.stamp]を削除
**ビルド時に再作成されるファイルのため、削除しても問題ない**


ChromeでFlutterアプリが動作することを確認


</details>

<details>

<summary>Windows環境構築</summary>

# Windows環境構築
## Google Chrome
Google Chromeがインストールされていないと、[flutter doctor -v]が終了しなかったため、インストール



## Git For Windows
### install
[Override the default branch name for new repositories]を選択 > [main]に変更
[Checkout as-is, commit as-is]を選択
他はdefaultで[Next] > [Finish]を押下


### コマンドプロンプトを開き、versionを確認
git --version


### Git初期設定
git config --global user.name "Namae Myoji"
git config --global user.email "_username_@example.com"



## TortoiseGit
### install
すべてデフォルトでインストール



## Flutter SDK
### install
[C:\Users\_username_\dev\flutter\]となる様に保存


### システム環境変数に以下を追加
PATH=%PATH%;"C:\Users\_username_\dev\flutter\bin"


### Flutterが利用可能になっていることを確認
コマンドプロンプトを開き、以下のコマンドを実行
```
flutter --version
flutter doctor -v
```
**10分程度、何も表示されずに処理に時間が掛かる可能性あり**



## OpenJDK21
[openjdk-21+35_windows-x64_bin.zip]
[C:\Users\_username_\dev\jdk-21\]となる様に保存


### システム環境変数に以下を追加
JAVA_HOME="C:\Users\_username_\dev\jdk-21"
PATH=%PATH%;"%JAVA_HOME%\bin"


### コマンドプロンプトを開き、versionを確認
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


### Extensions
以下を検索して[install]を押下
- Flutter
- Extension Pack for Java
- Gradle for Java
- Spring Boot Extension Pack



## MySQL
### install
[Server only]を選択、その他すべてデフォルトでインストール
[MySQL Root Password], [Repeat Password]: [_任意のパスワード_]を入力
[Execute]後、[Finish]ボタンが表示されたら、
[The configuration for MySQL Server 8.0.39 was successful.]と表示されたことを確認


### システム環境変数に以下を追加
PATH=%PATH%;"C:\Program Files\MySQL\MySQL Server 8.0\bin"


### コマンドプロンプトを開き、versionを確認
mysql --version


### 引き続きコマンドプロンプトでdatabaseを作成
コマンドプロンプトを開き、以下のコマンドを実行
```
mysql -u root -p
```

install時の[_任意のパスワード_]を入力
以下を入力

```
create user admin identified by '_任意のパスワード_';
create database fasse;
grant all on fasse.* to admin;
grant select, insert on fasse.* to admin;
quit
```

### VSCode Extensions
以下を検索して[install]を押下
- MySQL Shell for VS Code

左の[MySQL Shell for VS Code]を押下
[DB Connection Overview]を押下
右下に[Run Welcome Wizard]が表示されたら押下
```
The MySQL Shell for VS Code extension cannot run because the web certificate is not installed. Do you want to run the Welcome Wizard to install it?
Source: MySQL Shell for VS Code
```
指示に従って[VC_redist.x64.exe]のインストールが必要な環境もある
Wizardに従って証明書をインストール
VSCodeを再起動
左の[MySQL Shell for VS Code]を押下
[New Connection]を押下
以下を入力して[OK]を押下
```
Caption: fasse
Username: admin
```
左の[DATABASE CONNECTION] > [fasse]を右クリック > [Open New Database Connection]を押下
install時の[_任意のパスワード_]を入力


### [fasse]の[DB Notebook]が開いたらバージョンを確認
以下を入力し、[Ctrl + Enter]を押下
```
select version();
```


### 動作確認用のschema, dataを投入
以下を入力し、[Cmd + Enter]を押下
```
use fasse
[./src/test/resources/schema.sql]を開く > 全選択 > 貼り付け > [Cmd + Enter]を押下
[./src/test/resources/data.sql]を開く > 全選択 > 貼り付け > [Cmd + Enter]を押下
```


# Visual Studio Code 動作確認手順
## SpringBoot
[fasse_back]プロジェクトを[Git Clone]
[fasse_back]プロジェクトを[Visual Studio Code]で開く


### application.yaml設定
[src/main/resources/application.yaml]をコピーして[src/main/resources/application-local.yaml]を作成
以下を修正
```
_dbname_: fasse
_username_: admin
_password_: [_任意のパスワード_]
```

[src/main/java/com/example/fasse_back/FasseBackApplication.java]をデバッグ実行



## Flutter
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


### CORS対応
Flutter-SpringBootをローカル環境で連携すると、[CORS: Cross-Origin Resource Sharing]で止められるため、開発用に以下を修正

[C:\Users\_username_\dev\flutter\packages\flutter_tools\lib\src\web\chrome.dart]を開く
```
      '--disable-extensions',
      '--disable-web-security', // 開発用にこの行を追加
```

[C:\Users\_username_\dev\flutter\bin\cache\flutter_tools.stamp]を削除
**ビルド時に再作成されるファイルのため、削除しても問題ない**


ChromeでFlutterアプリが動作することを確認



# [MySQL Shell for VS Code]の証明書削除手順
Chrome > [設定] > [プライバシーとセキュリティ] > [セキュリティ] > [証明書の管理]を押下
[ローカル証明書] > [Windowsからインポートした証明書を管理する]を押下
[信頼されたルート証明機関] > [発行先: MySQL Shell Auto Generated CA Certificate]を選択 > [削除]を押下
警告されるが、これで削除できる。
再度、[Run Welcome Wizard]を実行すれば、再インストールされる。

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
- PDF出力
- ログイン
- ログアウト
- ログ出力
- オンデマンドバッチ



# テスト自動化
- SpringBootのテスト自動化
- Flutterのテスト自動化



# 教育目標
- SpringBootでMySQLからデータを抽出し、JSONデータを返却できること
- SpringBootでJSONデータをMySQLに登録できること
- FlutterでWebAPIのGETメソッドをコールし、返却されたJSONデータを表示できること
- FlutterでWebAPIのPOSTメソッドをコールし、JSONデータを送信できること

</details>
