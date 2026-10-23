#!/bin/sh
set -eu

REPO="Andre031222/jxmvc"
TAG="${JXMVC_VERSION:-}"
TOMCAT="10.1.31"

say()  { printf '%s\n' "$*"; }
fail() { printf 'jxmvc: %s\n' "$*" >&2; exit 1; }

need() {
  command -v java >/dev/null 2>&1 || fail "falta Java 17 o superior: https://adoptium.net"
  command -v mvn  >/dev/null 2>&1 || fail "falta Maven 3.9 o superior: https://maven.apache.org/download.cgi"
  major=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
  [ "${major:-0}" -ge 17 ] || fail "se necesita Java 17 o superior (encontrado: ${major:-desconocido})"
}

latest_tag() {
  curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" | sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' | head -1
}

install_core() {
  [ -n "$TAG" ] || TAG=$(latest_tag)
  [ -n "$TAG" ] || fail "no se pudo consultar el último release de $REPO"
  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT
  say "Descargando JxMVC $TAG desde https://github.com/$REPO"
  curl -fsSL "https://github.com/$REPO/archive/refs/tags/$TAG.tar.gz" | tar -xz -C "$work"
  core=$(find "$work" -maxdepth 2 -type d -name JxMVC.Core | head -1)
  [ -n "$core" ] || fail "el release $TAG no contiene JxMVC.Core"
  say "Compilando e instalando en el repositorio Maven local"
  mvn -q -DskipTests -f "$core/pom.xml" install
  VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' "$core/pom.xml" | head -1)
  say "Instalado jxmvc:jxmvc-core:$VERSION"
}

check_name() {
  printf '%s' "$1" | grep -Eq '^[a-z][a-z0-9-]*$' || fail "nombre inválido: usa minúsculas, números y guiones"
  [ ! -e "$1" ] || fail "ya existe '$1'"
}

new_app() {
  name="$1"
  mkdir -p "$name/src/main/java/app/controllers" "$name/src/main/resources" \
           "$name/src/main/webapp/WEB-INF/views/home" "$name/src/main/webapp/WEB-INF/views/shared"

  cat > "$name/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>app</groupId>
  <artifactId>$name</artifactId>
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
      <version>$VERSION</version>
    </dependency>
  </dependencies>

  <build>
    <finalName>$name</finalName>
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
              <version>$TOMCAT</version>
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
              <artifactId>$name</artifactId>
              <type>war</type>
              <properties><context>/</context></properties>
            </deployable>
          </deployables>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
EOF

  cat > "$name/src/main/webapp/WEB-INF/web.xml" <<'EOF'
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
EOF

  cat > "$name/src/main/resources/application.properties" <<'EOF'
jxmvc.controllers.package = app.controllers
jxmvc.profiles.active     = prod
jxmvc.pool.enabled        = false
EOF

  cat > "$name/src/main/java/app/controllers/HomeController.java" <<'EOF'
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
EOF

  cat > "$name/src/main/webapp/WEB-INF/views/home/index.jsp" <<EOF
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>$name</title>
</head>
<body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 4rem auto; padding: 0 1rem">
  <h1>$name</h1>
  <p>\${mensaje}</p>
  <p><a href="api/hola?nombre=JxMVC">Probar la API</a></p>
</body>
</html>
EOF

  cat > "$name/src/main/webapp/WEB-INF/views/shared/error.jsp" <<'EOF'
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Error ${jx_error_code}</title></head>
<body style="font-family: system-ui, sans-serif; max-width: 40rem; margin: 4rem auto; padding: 0 1rem">
  <h1>Error ${jx_error_code}</h1>
  <p>No se pudo completar la solicitud.</p>
</body>
</html>
EOF

  cat > "$name/.gitignore" <<'EOF'
target/
EOF

  cat > "$name/README.md" <<EOF
# $name

Aplicación creada con [JxMVC](https://github.com/$REPO) $VERSION.

\`\`\`bash
mvn package cargo:run     # descarga Tomcat $TOMCAT y abre http://localhost:8080
mvn package               # genera target/$name.war para desplegar en Tomcat 10.1+
\`\`\`
EOF

  say ""
  say "Proyecto creado en ./$name"
  say "  cd $name"
  say "  mvn package cargo:run"
  say "  abre http://localhost:8080"
}

need
case "${1:-}" in
  "")      install_core ;;
  new)     [ -n "${2:-}" ] || fail "uso: install.sh new <nombre>"; check_name "$2"; install_core; new_app "$2" ;;
  *)       fail "uso: install.sh [new <nombre>]" ;;
esac
