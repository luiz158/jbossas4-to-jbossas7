This folder is used if you want to deploy a lot of applications on severals groups of JBoss EAP 6.

There is a shell script which can deploy apps automatically :

# FRENCH DOC

  Script de déploiement d'une application (EAR) sur JBoss EAP 6.1 (JBoss AS 7.2) en mode domain avec gestion des ressources liées à l'environnement cible de déploiement (dev, integration, recette, formation, pre-èproduction, producton)

  Pré-requis : 
  ------------
  Afin de fonctionner, le script s'attend à trouver 
   1- AU MINIMUM
    a- dans le dossier de dépôt de l'application (-d) :
       -- un répertoire conf avec un fichier app-{env}.cfg avec les groupes de serveurs et les noms de serveurs cibles pour le déploiement
       -- un répertoire dédié à l'environnement (-e) avec une archive (EAR) correspondant à l'application
    b- un niveau au dessus du dossier de dépôt de l'application :
       -- un répertoire conf avec un fichier eap6-{env}.cfg avec les paramètres de connexion au domain controller EAP 6.1 
      
   2- SI L'APPLICATION UTILISE UNE CONF SPECIFIQUE A ENV (90% des cas) : 
    -- au même niveau que l'application EAR : une archive (TAR.GZ) contenant les fichiers templates (fichiers qui contiennent des variables à modifier selon l'environnement de déploiement)
   - dans le dossier conf :
    -- un fichier de configuration (CFG) de l'application par environnement qui contient les valeurs propres à l'environnement cible

  Fonctionnement : 
  ----------------
  1- Vérifiez les pré-requis
  2- Création des répertoires de travail (si non présents) ou nettoyage de ces répertoires s'ils sont déjà présents :
   |- conf ($globalConfDirectory)
       |-- eap6-integration.cfg
       |-- eap6-recette.cfg
   |- appDirectory ($appDirectory)
       |-- conf ($appConfDirectory)
            |- app-integration.cfg
            |- app-recette.cfg
       |-- env-integration ($appEnvDirectory)
            |-- history
            |-- tmp
            |-- toDeploy
            |-- app.ear
            |-- app.tar.gz
       |-- env-recette
            |-- history
            |-- tmp
            |-- toDeploy
            |-- app.ear
            |-- app.tar.gz
   
  3- Extraction de l'archive TAR.GZ dans le dossier tmp et remplacement des variables d'environnementi dans les fichiers templates (tmpl.*)
  4- Création d'un JAR avec les fichiers templates renommés (suppression tmpl.) et les valeurs mises à jour
  5- Copie de l'EAR et JAR dans toDeploy
  6- Execution des commandes du fichiers cli-command.txt avec JBoss CLI (JAR Client)
