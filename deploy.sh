./gradlew installDist

(cd server/build && rm -f install.zip && zip -r install.zip install)

scp server/build/install.zip root@188.245.102.242:apps/portfolio_api/

ssh root@188.245.102.242 'unzip -o /root/apps/portfolio_api/install.zip -d /root/apps/portfolio_api && rm /root/apps/portfolio_api/install.zip'