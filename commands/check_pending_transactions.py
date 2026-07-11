import os
import sys
import psycopg2
from psycopg2.extras import RealDictCursor
import requests

from config import DB_CONFIG, API_URL

# 1. Configuration (À adapter ou à passer via variables d'environnement)
"""DB_CONFIG = {
    "dbname": "votre_base_de_donnees",
    "user": "votre_utilisateur",
    "password": "votre_mot_de_passe",
    "host": "localhost",
    "port": "5432"
}"""

# API_URL = "https://votre-api.com/endpoint-callback"
API_HEADERS = {
    "Content-Type": "application/json",
    #"Authorization": "Bearer VOTRE_TOKEN_ICI" # Optionnel : si votre API est sécurisée
}

def check_and_notify_pending():
    connection = None
    cursor = None
    try:
        # 2. Connexion à la base de données PostgreSQL
        connection = psycopg2.connect(**DB_CONFIG)
        
        # RealDictCursor permet de récupérer les lignes sous forme de dictionnaire {colonne: valeur}
        cursor = connection.cursor(cursor_factory=RealDictCursor)
        
        # 3. Requête pour trouver les transactions en attente
        query = """
            SELECT external_ref, provider_ref, amount, currency 
            FROM mobile_money.transactions 
            WHERE status = 'PENDING_CONFIRMATION';
        """
        cursor.execute(query)
        transactions = cursor.fetchall()
        
        if not transactions:
            print("Aucune transaction avec le statut 'PENDING_CONFIRMATION' trouvée.")
            return

        print(f"{len(transactions)} transaction(s) trouvée(s). Envoi des requêtes POST...")

        # 4. Boucle sur chaque transaction trouvée pour envoyer le POST
        for tx in transactions:
            # Conversion du type NUMERIC (Decimal en Python) en float/string pour le JSON
            payload = {
                "external_ref": tx["external_ref"],
                "provider_ref": tx["provider_ref"],
                "amount": float(tx["amount"]), # Le JSON ne supporte pas le type Decimal directement
                "currency": tx["currency"]
            }
            
            try:
                # Envoi de la requête HTTP POST en application/json
                response = requests.post(API_URL, json=payload, headers=API_HEADERS, timeout=10)
                
                # Vérification du statut de la réponse HTTP
                if response.status_code in [200, 201, 202]:
                    print(f"✅ Notification envoyée avec succès pour external_ref: {payload['external_ref']}")
                    # Optionnel : Vous pourriez vouloir update le statut en base ici pour ne pas la traiter deux fois.
                else:
                    print(f"❌ Échec de la notification pour {payload['external_ref']}. Code HTTP: {response.status_code}, Réponse: {response.text}")
            
            except requests.exceptions.RequestException as e:
                print(f"💥 Erreur réseau lors de l'appel API pour {payload['external_ref']}: {e}")

    except Exception as error:
        print(f"🚨 Erreur lors de la connexion ou de l'exécution de la requête SQL: {error}")
        
    finally:
        # 5. Fermeture propre des ressources
        if cursor:
            cursor.close()
        if connection:
            connection.close()

if __name__ == "__main__":
    check_and_notify_pending()