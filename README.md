# Hubert

## Deployment

### Dokku

Create a Dokku instance on DigitalOcean: https://marketplace.digitalocean.com/apps/dokku.

Hostname: findka.com
[x] Use virtualhost naming for apps

ssh root@hub.findka.com
dokku apps:create hubert
dokku storage:mount hubert /var/lib/dokku/data/storage/hubert:/storage

git remote add dokku dokku@hub.findka.com:hubert
git push dokku master

