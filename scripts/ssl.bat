export PW=`pwgen -Bs 10 1`
echo $PW > ../public/ssl/password

export PW=`cat ../public/ssl/password`

# Create a self signed key pair root CA certificate.
keytool -genkeypair -v \
  -alias idolsca \
  -dname "CN=idolsCA, OU=TACC, O=TACC, L=Austin, ST=Texas, C=US" \
  -keystore ../public/ssl/idolsca.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 9999

# Export the idolsCA public certificate as idolsca.crt so that it can be used in trust stores.
keytool -export -v \
  -alias idolsca \
  -file ../public/ssl/idolsca.crt \
  -keypass:env PW \
  -storepass:env PW \
  -keystore ../public/ssl/idolsca.jks \
  -rfc
  
export PW=`cat ../public/ssl/password`

# Create a server certificate, tied to wrangler.tacc.utexas.edu:58888
keytool -genkeypair -v \
  -alias wrangler.tacc.utexas.edu:58888 \
  -dname "CN=wrangler.tacc.utexas.edu:58888, OU=TACC, O=TACC, L=Austin, ST=Texas, C=US" \
  -keystore ../public/ssl/wrangler.tacc.utexas.edu:58888.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 385
  
# Create a certificate signing request for wrangler.tacc.utexas.edu:58888
keytool -certreq -v \
  -alias wrangler.tacc.utexas.edu:58888 \
  -keypass:env PW \
  -storepass:env PW \
  -keystore ../public/ssl/wrangler.tacc.utexas.edu:58888.jks \
  -file ../public/ssl/wrangler.tacc.utexas.edu:58888.csr

# Tell idolsCA to sign the wrangler.tacc.utexas.edu:58888 certificate. Note the extension is on the request, not the
# original certificate.
# Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
keytool -gencert -v \
  -alias idolsca \
  -keypass:env PW \
  -storepass:env PW \
  -keystore ../public/ssl/idolsca.jks \
  -infile ../public/ssl/wrangler.tacc.utexas.edu:58888.csr \
  -outfile ../public/ssl/wrangler.tacc.utexas.edu:58888.crt \
  -ext KeyUsage:critical="digitalSignature,keyEncipherment" \
  -ext EKU="serverAuth" \
  -ext SAN="DNS:wrangler.tacc.utexas.edu" \
  -rfc

# Tell wrangler.tacc.utexas.edu:58888.jks it can trust idolsca as a signer.
keytool -import -v \
  -alias idolsca \
  -file ../public/ssl/idolsca.crt \
  -keystore ../public/ssl/wrangler.tacc.utexas.edu:58888.jks \
  -storetype jks \
  -storepass:env PW << EOF
yes
EOF

# Import the signed certificate back into wrangler.tacc.utexas.edu:58888.jks 
keytool -import -v \
  -alias wrangler.tacc.utexas.edu:58888 \
  -file ../public/ssl/wrangler.tacc.utexas.edu:58888.crt \
  -keystore ../public/ssl/wrangler.tacc.utexas.edu:58888.jks \
  -storetype JKS \
  -storepass:env PW

# List out the contents of wrangler.tacc.utexas.edu:58888.jks just to confirm it.  
# If you are using Play as a TLS termination point, this is the key store you should present as the server.
keytool -list -v \
  -keystore ../public/ssl/wrangler.tacc.utexas.edu:58888.jks \
  -storepass:env PW
