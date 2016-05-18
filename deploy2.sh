#!/bin/sh

function msg {
    echo "**************************************************************"
    echo "$1"
    echo "**************************************************************"
}

function error_exit {
    echo "_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_*_"
    echo "$1"
    exit 1
}

if [ -z "$1" ]; then
    echo "Usage: $0 <env> [unit-tests?(=true)] [branch]"
    exit 1
fi

set -e

START_TS=`date +%s`

CURRENT_BRANCH=`git symbolic-ref --short HEAD`
HARJA_ENV=harja-dev$1
BRANCH=$3
UNIT=$2

if [ -z "$BRANCH" ]; then
    BRANCH=$CURRENT_BRANCH
fi

echo "Alustetaan testikanta"
pushd vagrant
sh migrate_test.sh > /dev/null
popd

if [ -z "$UNIT" ] || [ "$UNIT" = true ]; then
  # msg "Voit estää unit testien ajamisen antamalla toiseksi parametriksi jotain muuta kuin true"
  # echo ""
  sh unit.sh
else
  msg "Who said you're not responsible? It doesn't matter that you were tricked, you know now that your actions inflicted grievous damage. Do you take responsibility for them or not? Well, what are you going to do?"
  sh unit.sh
fi

echo ""
echo "Deployaan branchin $BRANCH ympäristöön $HARJA_ENV"

git push $HARJA_ENV $BRANCH || error_exit "Push epäonnistui, tarkista että remote on olemassa: git remote add $HARJA_ENV ssh://root@$HARJA_ENV/opt/harja-repo"

pushd test_envs/upcloud
ansible-playbook deploy2.yml -i inventory/harjadev --extra-vars "harja_migrate_only=false harja_branch=$BRANCH" --limit $HARJA_ENV || error_exit "Deploy epäonnistui"
popd



msg "Deploy valmis palvelimelle $HARJA_ENV. Laitoin Harja Projekti HipChat-kanavalle tiedon asiasta."

# HipChat notifikaatio
CONFIG="room_id=914801&from=deploy2.sh&color=purple"
MESSAGE="$USER deployasi juuri uuden Harja-version haarasta $BRANCH palvelimelle <a href=\"https://$HARJA_ENV\">$HARJA_ENV</a>"
curl -d $CONFIG --data-urlencode "message=${MESSAGE}" 'https://api.hipchat.com/v1/rooms/message?auth_token=1c37e57158bf6a041a8a687b82e1bd&format=json'

# Slack notifikaatio
MESSAGE="$USER deployasi juuri uuden Harja-version haarasta $BRANCH palvelimelle <https://$HARJA_ENV>"
curl -d "payload={\"text\": \"${MESSAGE}\"}" https://hooks.slack.com/services/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

END_TS=`date +%s`
msg "Suorite kesti `echo "$END_TS-$START_TS"|bc` sekuntia."
