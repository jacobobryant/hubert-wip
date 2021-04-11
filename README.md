# Hubert

TODO add description

## Development

Run `./task dev` and `./task css`.

## Server setup

Copy `config-TEMPLATE` to `config`. Update vars in `config/prod.env` and
`config/task.env`.

Log in to DigitalOcean and create a droplet (Ubuntu 20.04 LTS). I usually do
Regular Intel at $10/month. Add monitoring. Make sure your SSH key is selected.
(If needed, go to Settings and add your SSH key, then start over). Set the
hostname to something distinctive. After the droplet is created, go to
Networking and point your domain to it (create an A record).

Run the setup script on your new droplet, replacing `example.com` with your
domain:

```bash
scp infra/setup.sh root@example.com:
ssh root@example.com
./setup.sh
reboot
```

From your local machine, add your server as a remote:

```bash
git remote add prod ssh://app@example.com/home/app/repo.git
```

## Deploy

Commit your changes locally, then run `./task deploy`.

## Monitoring

Run `./task logs` to view the systemd logs. Run `./task prod-repl` to connect
to the production nrepl server
