# 環境構築
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
- SQLTools
- SQLTools MySQL/MariaDB/TiDB

[左のSQLTools]を押下
[Add New Connection]を押下
[MySQL]を押下
以下を入力して[SAVE CONNECTION]を押下
```
Connection name: MYSQL
Database: fasse
Username: admin
```
[CONNECT NOW]を押下
> The extension 'SQLTools MySQL/MariaDB/TiDB' wants to sign in using SQLTools Driver Credentials
が表示されたら[Allow]を押下
install時の[_任意のパスワード_]を入力


### [MYSQL.session.sql]のエディタが開いたらバージョンを確認
以下を入力し、[Ctrl + E] > [Ctrl + E]を押下
```
select version();
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
