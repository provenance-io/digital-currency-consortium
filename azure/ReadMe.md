- Install Azure CLI : brew update && brew install azure-cli
- Azure Login : az login
- List locations : az account list-locations
- Create a resource group : az group create --name digitalCurrencyConsortium --location centralus
- Json return by previous step will be used as AZURE_CREDENTIALS
- Get subscription-id : cat ~/.azure/azureProfile.json
- Create Azure Service Principal for RBAC : az ad sp create-for-rbac --name "digitalCurrencyConsortiumApp" --role contributor --scopes /subscriptions/d4738521-9de7-4360-9c75-9644e292ce4a/resourceGroups/digitalCurrencyConsortium --sdk-auth
- Create Registry : az acr create --resource-group digitalCurrencyConsortium --name digitalCurrencyConsortiumRegistry --sku Basic
- Enable admin : az acr update -n digitalCurrencyConsortiumRegistry --admin-enabled true
- Regenerate login credentials for an Azure Container Registry : az acr credential renew --name digitalCurrencyConsortiumRegistry --password-name password --resource-group digitalCurrencyConsortium
- Add secret AZURE_CREDENTIALS in https://github.com/RadialTheory/digital-currency-consortium/settings/secrets/actions/new 
- Add secret REGISTRY_USERNAME,REGISTRY_PASSWORD in https://github.com/RadialTheory/digital-currency-consortium/settings/secrets/actions/new 
- az provider register --namespace Microsoft.ContainerInstance
Ref : https://github.com/marketplace/actions/deploy-to-azure-container-instances#build-and-deploy-a-nodejs-app-to-azure-container-instances