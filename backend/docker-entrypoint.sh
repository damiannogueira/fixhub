#!/bin/sh
set -eu

fail() {
    printf '%s\n' "Error al preparar TLS de MySQL: $1" >&2
    exit 1
}

ca_file=/etc/secrets/railway-ca.pem
[ -r "$ca_file" ] && [ -s "$ca_file" ] \
    || fail "falta railway-ca.pem o el usuario fixhub no puede leerlo."
command -v keytool >/dev/null 2>&1 || fail "keytool no esta disponible."

case "${DB_URL:-}" in
    jdbc:mysql://*) ;;
    *) fail "DB_URL debe ser una URL jdbc:mysql:// sin credenciales ni parametros." ;;
esac
case "$DB_URL" in
    *\?*|*\#*|*@*) fail "DB_URL debe indicar solo servidor, puerto y base, sin parametros ni credenciales." ;;
esac

umask 077
trust_dir=$(mktemp -d /tmp/fixhub-mysql-tls.XXXXXX) \
    || fail "no se pudo crear el directorio temporal del truststore."
trap 'rm -f "$trust_dir/mysql.p12"; rmdir "$trust_dir"' 0

TRUSTSTORE_PASSWORD=$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')
[ "${#TRUSTSTORE_PASSWORD}" -eq 64 ] \
    || fail "no se pudo generar la clave temporal del truststore."
export TRUSTSTORE_PASSWORD

keytool -importcert -noprompt -alias railway-mysql-ca \
    -file "$ca_file" -keystore "$trust_dir/mysql.p12" \
    -storetype PKCS12 -storepass:env TRUSTSTORE_PASSWORD >/dev/null 2>&1 \
    || fail "no se pudo importar railway-ca.pem como certificado CA."
chmod 0600 "$trust_dir/mysql.p12"

DB_URL="${DB_URL}?sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:${trust_dir}/mysql.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=${TRUSTSTORE_PASSWORD}&fallbackToSystemTrustStore=false"
export DB_URL
export SPRING_DATASOURCE_URL="$DB_URL"
unset TRUSTSTORE_PASSWORD

exec java -jar /app/backend-0.0.1-SNAPSHOT.jar
