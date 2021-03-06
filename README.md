# Liikenneviraston Harja järjestelmä #

Projekti on client/server, jossa serveri on Clojure sovellus (http-kit) ja
client on ClojureScript sovellus, joka käyttää Reagentia, OpenLayersiä ja Bootstrap CSSää.

Tietokantana PostgreSQL PostGIS laajennoksella. Hostaus Solitan infrassa.

Autentikointiin käytetään KOKAa.

## Hakemistorakenne ##

Harja repon hakemistorakenne:

- README                    (yleinen readme)

- src/                      (kaikki lähdekoodit)
  - cljc/                   (palvelimen ja asiakkaan jaettu koodi)
  - cljs/                   (asiakaspuolen ClojureScript koodi)
    - harja/asiakas/
      - ui/                 (yleisiä UI komponentteja ja koodia)
      - nakymat/            (UI näkymät)
      - kommunikointi.cljs  (serverikutsujen pääpiste)
      - main.cljs           (aloituspiste, jota sivun latauduttua kutsutaan)
  - cljs-{dev|prod}/        (tuotanto/kehitysbuildeille spesifinen cljs source)
      - harja/asiakas/
        - lokitus.cljs      (lokitus, tuotantoversiossa no-op, kehitysversiossa console.log tms)
  - clj/                    (palvelimen koodi)
    - harja/palvelin/
      - komponentit/        (Yleiset komponentit: tietokanta, todennus, HTTP-palvelin, jne)
      - lokitus/            (Logitukseen liittyvää koodia)
      - integraatiot/       (Integraatioiden toteutukset)
      - api/                (Harja API endpointit ja tukikoodi)
      - palvelut/           (Harja asiakkaalle palveluja tarjoavat EDN endpointit)
      - main.cljs           (palvelimen aloituspiste)

- (dev-)resources/          (web-puolen resurssit)
  - css/                    (ulkoiset css tiedostot)
  - js/                     (ulkoiset javascript tiedostot)


## Integraatiot

MULEsta on luovuttu, integraatiot suoraan backendistä.

## Tietokanta

Tietokannan määrittely ja migraatio (SQL tiedostot ja flyway taskit) ovat harja-repositorion kansiossa tietokanta

Ohjeet kehitysympäristön tietokannan pystytykseen Vagrantilla löytyvät tiedostosta `vagrant/README.md`

## Staging tietokannan sisällön muokkaus
* Lisää itsellesi tiedosto ~/.ssh/config johon sisällöksi:
Host harja-*-test
  ProxyCommand ssh harja-jenkins.solitaservices.fi -W %h:%p

Host harja-*-stg
  ProxyCommand ssh harja-jenkins.solitaservices.fi -W %h:%p

* Sourceta uusi config tai avaa uusi terminaali-ikkuna.

* Avaa VPN putki.

* Luo itsellesi SSH-avainpari ja pyydä tuttuja laittamaan julkinen avain palvelimelle.

ssh -L7777:localhost:5432 harja-dfb1-stg
 * Luo yhteys esim. käyttämäsi IDE:n avulla,
    * tietokanta: harja, username: flyway salasana: kysy tutuilta

## Testipalvelimen tietokannan päivitys, vanha ja huono tapa, mutta säilyköön ohje jälkipolville:
 * Avaa VPN putki <br/>
 <code>
    ssh harja-jenkins.solitaservices.fi
    [jarnova@harja-jenkins ~]$ sudo bash <br/>
    [root@harja-jenkins jarnova]# su jenkins <br/>
    bash-4.2$ ssh harja-db1-test <br/>
    Last login: Mon Mar 16 15:23:22 2015 from 172.17.238.100 <br/>
    [jenkins@harja-db1-test ~]$ sudo bash <br/>
    [root@harja-db1-test jenkins]# su postgres <br/>
    bash-4.2$ psql harja <br/>
</code>
 * Tee temput

## Autogeneroi nuolikuvat SVG:nä
Meillä on nyt Mapen tekemät ikonit myös nuolille, joten tälle ei pitäisi olla tarvetta.
Jos nyt kuitenkin joku käyttää, niin kannattaa myös varmistaa että alla määritellyt värit osuu
puhtaat -namespacessa määriteltyihin.

(def varit {"punainen" "rgb(255,0,0)"
            "oranssi" "rgb(255,128,0)"
            "keltainen" "rgb(255,255,0)"
            "lime" "rgb(128,255,0)"
	    "vihrea" "rgb(0,255,0)"
 	    "turkoosi" "rgb(0,255,128)"
 	    "syaani" "rgb(0,255,255)"
 	    "sininen" "rgb(0,128,255)"
 	    "tummansininen" "rgb(0,0,255)"
 	    "violetti" "rgb(128,0,255)"
 	    "magenta" "rgb(255,0,255)"
 	    "pinkki" "rgb(255,0,128)"})

(for [[vari rgb] varit]
  (spit (str "resources/public/images/nuoli-" vari ".svg")
  	(str "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 6 9\" width=\"20px\" height=\"20px\">
   <polygon points=\"5.5,5 0,9 0,7 3,5 0,2 0,0 5.5,5\" style=\"fill:" rgb ";\" />
</svg>")))


## Konvertoi SVG kuvia PNG:ksi

Käytä inkscape sovellusta ilman UI:ta. Muista käyttää täysiä tiedostopolkuja:
> /Applications/Inkscape.app/Contents/Resources/script --without-gui --export-png=/Users/minä/kuva/jossain/image.png /Users/minä/kuva/jossain/image.svg

Fish shellissä koko hakemiston kaikkien kuvien konvertointi:

kun olet hakemistossa, jonka svg kuvat haluat muuntaa:

> for i in *.svg; /Applications/Inkscape.app/Contents/Resources/script --without-gui --export-png=(pwd)/(echo $i | sed 's/\.[^.]*$//').png (pwd)/$i; end

## Aja cloveragelle testikattavuusraportti
Hae työkalu: https://github.com/jarnovayrynen/cloverage
Työkalun cloverage/cloverage kansiossa aja "lein install"
Harjan juuressa aja "env CLOVERAGE_VERSION=1.0.8-SNAPSHOT lein cloverage"

## Tietokantadumpin ottaminen omalle koneelle

### STG

Yksinkertainen tapa ottaa pakattu dumppi:

> ssh harja-db1-stg "sudo -u postgres pg_dump -v -Fc -T integraatioviesti -T liite harja" > tietokanta/harja-stg-dump

Tämä saattaa kuitenkin mystisesti kaatua kesken siirron.
Vaihtoehtoinen tapa SCP:llä:

Huom. voit olla välittämättä virheilmoituksesta: could not change directory to "/home/mikkoro": Permission denied. Kopiointi tehdään silti.

> ssh harja-db1-stg
> sudo -u postgres pg_dump -v -Fc harja > /tmp/harja-stg-dump
> mv /tmp/harja-stg-dump /home/<omatunnus>/harja-stg-dump
> exit
> scp <omatunnus>@harja-db1-stg:/home/<omatunnus>/harja-stg-dump /Users/<omatunnus>/Desktop/harja-stg-dump

Käy poistamassa dumppi kotihakemistostasi

Dumppi on nyt siirretty työpöydällesi. Siirrä se haluamaasi paikkaan.

### harja-test

Harja-test dumpin voi ottaa samalla logiikalla:

> ssh -L 7778:localhost:5432 harja-db1-test
> sudo -u postgres pg_dump harja > /tmp/harja-stg-dump.sql
> mv /tmp/harja-test-dump.sql /home/<omatunnus>/harja-test-dump.sql
> exit
> scp <omatunnus>@harja-db1-test:/home/<omatunnus>/harja-test-dump.sql /Users/<omatunnus>/Desktop/harja-test-dump.sql
> kopioi dumppi harja/tietokanta kansioon

### Dumpin käyttöönotto

Sulje oma REPL ettei yhteyksiä vagrant kantaan ole.
Mene vagrant-kansioon ja aja komennot:

> vagrant ssh
> sudo -u postgres psql
> (sulje kaikki kantaa käyttävät sovellukset)
> drop database harja;
> create database harja;
> poistu <ctrl-d>
> pg_restore -Fc -C /harja-tietokanta/harja-stg-dump | sudo -u postgres psql

Valmis!

## Kirjautuminen

Harja käyttää liikenneviraston extranetista tulevia headereita kirjautumiseen.
Käytä ModHeader tai vastaavaa asettaaksesi itselle oikeudet paikallisessa ympäristössä.

Oikeudet on määritelty tiedostossa resources/roolit.xslx: 1. välilehti kertoo oikeudet, 2. välilehti roolit
Harjan harja.domain.oikeudet.makrot luo Excelin pohjalta roolit käytettäväksi koodista.
Käyttäjällä voi olla useita rooleja. Oikeustarkistuksia tehdään sekä frontissa että backissä. Frontissa yleensä
piilotetaan tai disabloidaan kontrollit joihin ei ole oikeutta. Tämän lisäksi backissä vaaditaan
luku- ja/tai kirjoitusoikeus tietyn tiedon käsittelyyn.

Seuraavat headerit tuettuna:
* OAM_REMOTE_USER: käyttäjätunnus, esim. LX123123
* OAM_GROUPS: pilkulla erotettu lista ryhmistä (roolit ja niiden linkit), esim "Jarjestelmavastaava"
tai <urakan-SAMPO-ID>_ELY_Urakanvalvoja tai Urakoitisijan laatupäällikkö <urakoitsijan-ytunnus>_Laatupaallikko
* OAM_ORGANIZATION: Organisaation nimi, esim. "Liikennevirasto" tai "YIT Rakennus Oy"
* OAM_DEPARTMENTNUMBER: Organisaation ELYNUMERO, esim. 12 (POP ELY)
* OAM_USER_FIRST_NAME: Etunimi
* OAM_USER_LAST_NAME: Sukunimi
* OAM_USER_MAIL: Sähköpostiosoite
* OAM_USER_MOBILE: Puhelinnumero

Staging-ympäristössä voidaan lisäksi testata eri rooleja testitunnuksilla,
jotka löytyvät toisesta Excelistä, mitä ei ole Harjan repossa (ei salasanoja repoon).
