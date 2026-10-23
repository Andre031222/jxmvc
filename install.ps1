param([string]$Command = "", [string]$Name = "")

$ErrorActionPreference = "Stop"
$Repo = "Andre031222/jxmvc"
$Tomcat = "10.1.31"
$Tag = $env:JXMVC_VERSION

function Fail([string]$msg) { Write-Host "jxmvc: $msg" -ForegroundColor Red; exit 1 }

function Test-Requirements {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Fail "falta Java 17 o superior: https://adoptium.net" }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Fail "falta Maven 3.9 o superior: https://maven.apache.org/download.cgi" }
    $line = (& java -version 2>&1 | Select-Object -First 1).ToString()
    if ($line -notmatch 'version "(\d+)') { Fail "no se pudo leer la versión de Java" }
    if ([int]$Matches[1] -lt 17) { Fail "se necesita Java 17 o superior (encontrado: $($Matches[1]))" }
}

function Install-Core {
    if (-not $script:Tag) { $script:Tag = (Invoke-RestMethod "https://api.github.com/repos/$Repo/releases/latest").tag_name }
    if (-not $script:Tag) { Fail "no se pudo consultar el último release de $Repo" }
    $work = Join-Path ([IO.Path]::GetTempPath()) ("jxmvc-" + [Guid]::NewGuid())
    New-Item -ItemType Directory -Path $work | Out-Null
    try {
        Write-Host "Descargando JxMVC $script:Tag desde https://github.com/$Repo"
        $zip = Join-Path $work "src.zip"
        Invoke-WebRequest "https://github.com/$Repo/archive/refs/tags/$script:Tag.zip" -OutFile $zip
        Expand-Archive $zip -DestinationPath $work
        $core = Get-ChildItem $work -Directory -Recurse -Depth 1 -Filter "JxMVC.Core" | Select-Object -First 1
        if (-not $core) { Fail "el release $script:Tag no contiene JxMVC.Core" }
        Write-Host "Compilando e instalando en el repositorio Maven local"
        & mvn -q -DskipTests -f (Join-Path $core.FullName "pom.xml") install
        if ($LASTEXITCODE -ne 0) { Fail "falló la compilación de JxMVC.Core" }
        $script:Version = ([xml](Get-Content (Join-Path $core.FullName "pom.xml"))).project.version
        Write-Host "Instalado jxmvc:jxmvc-core:$script:Version"
    } finally {
        Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Write-File([string]$path, [string]$content) {
    New-Item -ItemType Directory -Path (Split-Path $path) -Force | Out-Null
    $text = $content.Replace("`r`n", "`n")
    if (-not $text.EndsWith("`n")) { $text += "`n" }
    [IO.File]::WriteAllText($path, $text, [Text.UTF8Encoding]::new($false))
}

function Test-Name([string]$app) {
    if ($app -cnotmatch '^[a-z][a-z0-9-]*$') { Fail "nombre inválido: usa minúsculas, números y guiones" }
    if (Test-Path $app) { Fail "ya existe '$app'" }
}

function New-App([string]$app) {
    $root = Join-Path (Get-Location) $app
    $v = $script:Version

    Write-File "$root/pom.xml" @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app</groupId>
  <artifactId>$app</artifactId>
  <version>1.0.0</version>
  <packaging>war</packaging>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <failOnMissingWebXml>false</failOnMissingWebXml>
  </properties>

  <dependencies>
    <dependency>
      <groupId>jakarta.platform</groupId>
      <artifactId>jakarta.jakartaee-api</artifactId>
      <version>10.0.0</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>jxmvc</groupId>
      <artifactId>jxmvc-core</artifactId>
      <version>$v</version>
    </dependency>
  </dependencies>

  <build>
    <finalName>$app</finalName>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.12.1</version>
        <configuration>
          <release>17</release>
          <compilerArgs><arg>-parameters</arg></compilerArgs>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-war-plugin</artifactId>
        <version>3.4.0</version>
      </plugin>
      <plugin>
        <groupId>org.codehaus.cargo</groupId>
        <artifactId>cargo-maven3-plugin</artifactId>
        <version>1.10.15</version>
        <configuration>
          <container>
            <containerId>tomcat10x</containerId>
            <artifactInstaller>
              <groupId>org.apache.tomcat</groupId>
              <artifactId>tomcat</artifactId>
              <version>$Tomcat</version>
              <type>zip</type>
            </artifactInstaller>
            <systemProperties>
              <jxmvc.profiles.active>dev</jxmvc.profiles.active>
            </systemProperties>
          </container>
          <configuration>
            <properties>
              <cargo.servlet.port>8080</cargo.servlet.port>
              <cargo.logging>low</cargo.logging>
            </properties>
          </configuration>
          <deployables>
            <deployable>
              <groupId>app</groupId>
              <artifactId>$app</artifactId>
              <type>war</type>
              <properties><context>/</context></properties>
            </deployable>
          </deployables>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
"@

    Write-File "$root/src/main/webapp/WEB-INF/web.xml" @'
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
    <servlet>
        <servlet-name>MainLxServlet</servlet-name>
        <servlet-class>jxmvc.core.MainLxServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>MainLxServlet</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
'@

    Write-File "$root/src/main/resources/application.properties" @'
jxmvc.controllers.package = app.controllers
jxmvc.profiles.active     = prod
jxmvc.pool.enabled        = false
'@

    Write-File "$root/src/main/java/app/controllers/HomeController.java" @'
package app.controllers;

import java.util.Map;
import jxmvc.core.ActionResult;
import jxmvc.core.JxController;
import jxmvc.core.JxMapping.*;

@JxControllerMain
@JxControllerMapping("home")
public class HomeController extends JxController {

    @JxGetMapping("index")
    public ActionResult index() {
        model.setVar("mensaje", "Tu aplicación JxMVC está funcionando.");
        return view("home/index");
    }

    @JxGetMapping("/api/hola")
    public ActionResult hola() {
        String nombre = param("nombre");
        return json(Map.of("hola", nombre == null || nombre.isBlank() ? "mundo" : nombre));
    }
}
'@

    Write-File "$root/src/main/webapp/WEB-INF/views/home/index.jsp" (@'
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>__APP__</title>
</head>
<body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 4rem auto; padding: 0 1rem">
  <h1>__APP__</h1>
  <p>${mensaje}</p>
  <p><a href="api/hola?nombre=JxMVC">Probar la API</a></p>
</body>
</html>
'@).Replace("__APP__", $app)

    Write-File "$root/src/main/webapp/WEB-INF/views/shared/error.jsp" @'
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Error ${jx_error_code}</title></head>
<body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 4rem auto; padding: 0 1rem">
  <h1>Error ${jx_error_code}</h1>
  <p>No se pudo completar la solicitud.</p>
</body>
</html>
'@

    Write-File "$root/.gitignore" "target/`n"

    Write-File "$root/README.md" @"
# $app

Aplicación creada con [JxMVC](https://github.com/$Repo) $v.

``````bash
mvn package cargo:run     # descarga Tomcat $Tomcat y abre http://localhost:8080
mvn package               # genera target/$app.war para desplegar en Tomcat 10.1+
``````
"@

    Write-Host ""
    Write-Host "Proyecto creado en .\$app"
    Write-Host "  cd $app"
    Write-Host "  mvn package cargo:run"
    Write-Host "  abre http://localhost:8080"
}

Test-Requirements
switch ($Command) {
    ""      { Install-Core }
    "new"   { if (-not $Name) { Fail "uso: install.ps1 new <nombre>" }; Test-Name $Name; Install-Core; New-App $Name }
    default { Fail "uso: install.ps1 [new <nombre>]" }
}
