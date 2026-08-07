# 草花互动抽奖服务器 - 项目记忆

## 运行方式
```bash
# 编译
cd RaffleDrawing
"/c/Program Files/Java/jdk-26.0.1/bin/java" -cp ".mvn/wrapper/apache-maven-3.9.8/boot/plexus-classworlds-2.8.0.jar" -Dclassworlds.conf=".mvn/wrapper/apache-maven-3.9.8/bin/m2.conf" -Dmaven.home=".mvn/wrapper/apache-maven-3.9.8" -Dmaven.multiModuleProjectDirectory="C:\Users\caohua\Desktop\IDEAproject\RaffleDrawing" org.codehaus.plexus.classworlds.launcher.Launcher clean package -Dmaven.test.skip=true

# 运行
"/c/Program Files/Java/jdk-26.0.1/bin/java" -jar target/raffle-drawing-1.0.0.jar --server.port=8080
```

## 管理员账号
- 账号ID: chfz-00000000
- 姓名: 管理员
- 默认在 DataInitializer 中创建

## 登录方式
- 无需注册，页面自动生成唯一账号ID (chfz-XXXXXXXX)
- 老用户可粘贴已有账号ID
- 姓名选填，首次登录即自动创建账号

## 注意事项
- 数据库: 本机 MySQL 8.0 (数据库名 RaffleDrawing, root/caohuamoyu)
- `ddl-auto=update`，安全增量更新，不删数据
- Spring Boot 3.3.2 必须使用 jakarta.* 包名（非 javax.*）
- `/tmp` 在 Git Bash Windows 下不可用，curl cookies 需写到项目目录
