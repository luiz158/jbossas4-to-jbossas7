#!/bin/bash
# 
# Usage : $0 -n {nomApplication} -d {dossierDepotApp} -e {environnementCible} -c {controller} -u {user} -p {password}
#
#  Script de déploiement d'une application (EAR) sur JBoss EAP 6.1 (JBoss AS 7.2) en mode domain avec gestion des ressources liées à l'environnement cible de déploiement (dev, integration, recette, formation, pre-èproduction, producton)
#
#  Pré-requis : 
#  ------------
#  Afin de fonctionner, le script s'attend à trouver au minimum (-d) :
#   - dans le dossier de dépôt de l'application :
#    -- une archive (EAR) correspondant à l'application
#    SI APP UTILISE UNE CONF SPECIFIQUE A ENV : 
#    -- une archive (TAR.GZ) contenant les fichiers templates (fichiers qui contiennent des variables à modifier selon l'environnement de déploiement)
#   - dans le dossier conf :
#    -- un fichier de configuration (CFG) de l'application par environnement qui contient les valeurs propres à l'environnement cible
#
#  Fonctionnement : 
#  ----------------
#  1- Vérifiez les pré-requis
#  2- Création des répertoires de travail (si non présents) ou nettoyage de ces répertoires s'ils sont déjà présents :
#   - dossierDepotLivrable
#    |-- archives 
#    |-- conf 
#    |-- tmp
#    |-- toDeploy
#   
#  3- Extraction de l'archive TAR.GZ dans le dossier tmp et remplacement des variables d'environnementi dans les fichiers templates (tmpl.*)
#  4- Création d'un JAR avec les fichiers templates renommés (suppression tmpl.) et les valeurs mises à jour
#  5- Copie de l'EAR et JAR dans toDeploy et execution du script JBoss CLI
#  6- Si déploiement OK => copie dans archives
#
#  Auteur : mgreau
#
###########


#Paramètres pour la connexion au domain controller
EAP6_DOMAIN_CONTROLLER_IP="devrh6"
EAP6_DOMAIN_CONTROLLER_PORT="9999"
EAP6_DOMAIN_CONTROLLER_USER="admin"
EAP6_DOMAIN_CONTROLLER_PWD="jboss2013&"

#répertoire de travail pour les templates
tmpDirectory="tmp"
#repertoire des archives à déployer
toDeployDirectory="toDeploy"
#repertoires de l'historique de déploiement
historyDirectory="history"
#répertoire de conf propre à l'application
appConfDirectory="conf"
#répertoire de conf globale à l'environnement
globalConfDirectory="../conf"

debug=false;


function usage {
    sed -n '2,/^$/ s/^#//p' "$0" | sed "s~\$0~${0}~" >&2
    exit 4
}


#iRécupère les options
while getopts n:d:e:c:u:p:X opt; do
  case $opt in
      n) appName="$OPTARG" ;;
      d) appDirectory="$OPTARG" ;;
      e) environment="$OPTARG" ;;
      c) controller="$OPTARG" ;;
      u) user="$OPTARG" ;;
      p) password="$OPTARG" ;;
      X) debug=true ;;
 esac;
done

#Vérifie la présence de paramètres
if [ $# -eq 0 ]; then
    usage;
    exit;
fi

#Log des messages en cas de mode debug activé
function log {
  if [ $# -eq 1 ] && [ $debug = true ]; then
      echo "[DEBUG] $1";
  fi
}
#
#Verifie que tous les paramètres sont présents
function checkParams {
    if [ -z "$appName" ]; then
         echo "L'argument pour le nom de l'application (-n) n'est pas précisé."
	 exit;
    fi
    if [ -z "$appDirectory" ]; then
         echo "L'argument pour le dossier de dépôt de l'application (-d) n'est pas précisé."
	 exit;
    fi
    if [ -z "$environment" ]; then
        echo "L'argument pour l'environnement cible (-e) n'est pas précisé."
	exit;
    fi
    if [ -z "$controller" ]; then
        echo "L'argument pour le controller (-c) n'est pas précisé."
	exit;
    fi
    if [ -z "$user" ]; then
        echo "L'argument pour le  user (-u) n'est pas précisé."
	exit;
    fi
    if [ -z "$password" ]; then
        echo "L'argument pour le password (-p) n'est pas précisé."
	exit;
    fi
   log "Paramètres de la commande => app name : $appName / dossier de dépôt : $appDirectory / environnement cible : $environment / controller : $controller ($user / $password)"
}

#Vérifie la présence des fichiers
function checkPrerequisites {
  cd $appDirectory || usage

  #Transforme les chemins relatifs en absolue
  appDirectory="$(pwd)"
  tmpDirectory=$appDirectory"/"$tmpDirectory
  toDeployDirectory=$appDirectory"/"$toDeployDirectory
  historyDirectory=$appDirectory"/"$historyDirectory
  appConfDirectory=$appDirectory"/"$appConfDirectory
  globalConfDirectory=$appDirectory"/"$globalConfDirectory
  
  #vérifier presence dossier conf
  if [ ! -d "./conf" ]; then
     echo "Le dossier conf est obligatoire."
     usage
  fi

  ear=$(find . -maxdepth 1 -type f -name "*.ear")
  targz=$(find . -maxdepth 1 -type f -name "*.tar.gz")
  appConfFile=$(find "$appConfDirectory" -maxdepth 1 -type f -name "*-$environment.cfg")

  if [ ! -f "$ear" ] || [ ! -r "$ear" ]; then
	echo "Le fichier \"$ear\" est inutilisable." >&2
	usage
  fi
  #on cherche un fichier de conf global à plusieurs apps
  #on le concatène avec le fichier spécifique à l'application si necessaire 
  globalConfFile="$globalConfDirectory/eap6-$environment.cfg"
  if [ -f "$appConfFile" ]; then
	dos2unix $appConfFile
	conf="$(cat "$appConfFile")"$'\n\n'"$(cat "$globalConfFile")"
  else
        conf="$(cat "$globalConfFile")"
  fi
  log "Configuration :  $conf"

  if [ -z "$conf" ]; then
      echo "Aucune configuration n'a pu être chargée." >&2
      usage
 fi

}

# - création des dossiers necessaires
# - clean des répertoires de travail
function manageDirectories {
  #vérifier presence dossier de travail tmp
  if [ ! -d "$tmpDirectory" ]; then
     mkdir "$tmpDirectory"
  else
     rm -rf $tmpDirectory/* 
  fi

  #vérifier presence dossier de travail tmp
  if [ ! -d "$toDeployDirectory" ]; then
     mkdir "$toDeployDirectory"
  else
     rm -rf $toDeployDirectory/* 
  fi

  if [ ! -d "$historyDirectory" ]; then
     mkdir "$historyDirectory"
  fi
}

# decompresse le TAR.GZ et modifie les valeurs
function manageTemplates {
  # Decompresse la pièce la jointe
  if [ -f "$targz" ]; then
    tar -xzvf "$targz" -C "$tmpDirectory"

    scriptSedTxt="$(tr -d '\r' <<<"$conf" | sed 's/[.&]/\\\0/g' | sed -nr 's#^([^=]*[^[:blank:]])[[:blank:]]*=[[:blank:]]*(.*)$#s~{\1}~\2~g#p')"
    scriptSedXml="$(sed 's/\&/\&amp;/g; s/</\\\&lt;/g; s/>/\\\&gt;/g' <<<"$scriptSedTxt")"
    scriptSedSh="$(sed 's/[$"]/\\\0/g' <<<"$scriptSedTxt")"
    
    #Traitements des fichiers templates
    while read fichier; do
        fichierSource="$fichier"
        fichierDestination="$toDeployDirectory/$(sed -r 's#^.*tmpl.(.*)$#\1#' <<<$fichier)"
        #remplacement des variables
        type_fichier=$(file -k "$fichierSource")
        if grep -F XML <<<"$type_fichier" &>/dev/null; then
            sed "$scriptSedXml" "$fichierSource" >"$fichierDestination"
        elif grep -Fi 'shell script' <<<"$type_fichier" &>/dev/null; then
           sed "$scriptSedSh" "$fichierSource" >"$fichierDestination"
        else
           sed "$scriptSedTxt" "$fichierSource" >"$fichierDestination"
        fi
	echo "Fichier template \"$fichier\" traité."
    done < <(find . -name "tmpl.*")
  fi
}

function createDeploymentFiles {
   #toDeployDirectory="C:\\Developpement\\git\\jboss\\test-shell\\toDeploy"
   cd $toDeployDirectory && jar cvf "$appName-conf.jar" .  
   cd ..
   cp $ear $toDeployDirectory

   appArchiveToDeploy="$toDeployDirectory/$ear"
   appJARConfName="$appName-conf.jar"
   appJARConfToDeploy="$toDeployDirectory/$appJARConfName"
}

#Utiliser JBOSS CLI pour deployer
function deployApplication {

  #bug cydrive
  #appJARConfToDeploy="C:\Developpement\\git\\jboss\\test-shell\\toDeploy\\$appName-conf.jar"
  #cmdUndeployJARConf="undeploy $appName-conf.jar --server-groups=$serverGroups"
  #cmdDeployJARConf="deploy $appJARConfToDeploy --server-groups=$serverGroups"

  #Replace variable cli-commands.txt
   #manage cli-commands.txt file
  
   conf="$(cat "$conf")"$'\n\n'
   log "conf ? $conf"
   scriptSediCliTxt="$(tr -d '\r' <<<"$conf" | sed 's/[.&]/\\\0/g' | sed -nr 's#^([^=]*[^[:blank:]])[[:blank:]]*=[[:blank:]]*(.*)$#s~{\1}~\2~g#p')"
   cliTmpFile=$(find "$tmpDirectory" -maxdepth 1 -type f -name "tmpl.cli-commands.txt")
   cliTargetFilen="$toDeployDirectory/$(sed -r 's#^.*tmpl.(.*)$#\1#' <<<$cliTmpFile)"
   sed "$scriptSedCliTxt" "$cliTmpFile" >"$cliTargetFile"

  #Execute liste de commandes
  $appDirectory/../as7-cli-helloworld.jar --connect --controller=${controller} --user=${user} --password=${password} --file="$toDeployDirectory/cli-commands.txt"
  
  #echo "Suppression déploiement JAR de conf : $cmdUndeployJARConf"
  #$JBOSS_HOME/bin/jboss-cli.sh --controller=${EAP6_DOMAIN_CONTROLLER_IP}:${EAP6_DOMAIN_CONTROLLER_PORT} --user=${EAP6_DOMAIN_CONTROLLER_USER} --password=${EAP6_DOMAIN_CONTROLLER_PWD} --commands="connect,$cmdUndeployJARConf"
  
  #echo "Déploiement JAR de conf : $cmdDeployJARConf"
  #$JBOSS_HOME/bin/jboss-cli.sh --controller=${EAP6_DOMAIN_CONTROLLER_IP}:${EAP6_DOMAIN_CONTROLLER_PORT} --user=${EAP6_DOMAIN_CONTROLLER_USER} --password=${EAP6_DOMAIN_CONTROLLER_PWD}  --commands="connect,$cmdDeployJARConf"
  #echo "Déploiement OK"
  

}

echo "----DEBUT DEPLOIEMENT--------"
checkParams
checkPrerequisites
manageDirectories
manageTemplates
createDeploymentFiles
deployApplication











