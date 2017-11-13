
Compiler le programme avec ant.
Connectez-vous en ssh à toutes vos machines.
Créez un fichier repertoriant les serveurs a utiliser avec en premiere ligne le nombre de serveurs total, puis pour chaque serveur une ligne avec sa capacité max et son adresse ip.
Exemple :
	4
	3 132.207.12.33
	3 132.207.12.34
	3 132.207.12.35
	3 132.207.12.36
Lancer les serveurs grâce au script calculateur avec en argument sa capacité max et son pourcentage de malice.
Lancer enfin le repartiteur grâce au scrpt repartiteur avec en argument le fichier d'opérations, true ou false pour le mode sécurisé ou non et le fichier serveur.