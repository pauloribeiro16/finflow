# Política de Privacidade — Finflow (Finanças Pessoais)

*Última atualização: 11 de Novembro de 2026*

A sua privacidade é a nossa prioridade absoluta. O **Finflow** guarda os seus dados financeiros apenas no seu dispositivo: as suas informações financeiras nunca saem dele sem a sua ação expressa. A aplicação só se liga à internet para verificar e descarregar atualizações e, se o pedir, para obter cotações de mercado (ver secção 3).

---

## 1. Dados Recolhidos e Armazenamento
- **Dados Financeiros:** As transações, categorias, orçamentos, metas de poupança e posições de património introduzidas são guardadas **exclusivamente na base de dados local** do seu dispositivo Android.
- **Encriptação Local:** A base de dados local é protegida e encriptada com o padrão **SQLCipher (AES-256)**.
- **Sem Servidores Externos:** Não mantemos servidores de base de dados, nem sincronizamos os seus movimentos financeiros para a nuvem.
- **Captura de Notificações (opcional):** Se conceder acesso às notificações, o Finflow lê apenas notificações de apps bancárias e de pagamento conhecidas, extrai o montante e o comerciante e guarda-os como movimento pendente na base de dados encriptada. Só quando não é possível identificar o comerciante é que o texto da notificação (até 300 caracteres) fica guardado nesse movimento pendente, para o poder rever; é apagado quando aprova ou descarta o movimento, e os pendentes com mais de 30 dias são apagados automaticamente. Pode desativar a captura a qualquer momento em Conta.

## 2. Bloqueio da Aplicação
- Caso ative o bloqueio, a validação (biometria ou PIN/padrão do telemóvel) é efetuada diretamente pelo sistema operativo Android (**Android BiometricPrompt**).
- O Finflow **nunca** tem acesso nem armazena as suas impressões digitais ou dados biométricos.

## 3. Ligações à Internet e Terceiros
- **Verificação de Atualizações:** Ao abrir, a aplicação contacta o **GitHub** (`api.github.com`) para saber se existe uma versão nova; ao atualizar, descarrega o ficheiro de instalação a partir de `github.com` / `githubusercontent.com`. Nestes pedidos o GitHub recebe, como em qualquer ligação, o endereço IP do dispositivo e a identificação técnica da app (`Finflow-App-Android`). **Nenhum dado financeiro ou pessoal é enviado.** A verificação automática pode ser desativada em Conta → Versão. Consulte a política de privacidade do GitHub para saber como trata estes registos.
- **Cotações de Mercado (opcional):** Se guardar um ticker num investimento (por exemplo `LQQ.PA`), pode pedir ao Finflow a cotação atual. O pedido é feito ao **Yahoo Finance** (`query1.finance.yahoo.com`) e inclui apenas o ticker; o Yahoo recebe também o endereço IP do dispositivo e a identificação técnica da app (`Finflow-App-Android`). **Não são enviadas quantidades, valores, nomes nem outros dados.** Por omissão, a consulta só acontece quando toca em "Cotações" (com aviso na primeira vez); a atualização automática diária só é ativada se o escolher, em Conta → Versão, e pode ser desligada a qualquer momento. Consulte a política de privacidade do Yahoo para saber como trata estes registos. As cotações são estimativas sem garantia.
- **Zero Partilha:** Não partilhamos, vendemos ou transferimos os seus dados financeiros ou pessoais para terceiros.
- **Zero Anúncios e Rastreio:** O Finflow não inclui SDKs de publicidade, nem ferramentas de telemetria invasiva ou rastreadores de comportamento.

## 4. Cópias de Segurança (Backups)
- As funcionalidades de exportação (JSON/CSV) só criam ficheiros no local que o utilizador escolher no seletor do Android (armazenamento do dispositivo ou um fornecedor de nuvem pessoal, ex.: Google Drive). Estes ficheiros **não são encriptados**: guarde-os num local seguro.
- Antes de cada atualização, é guardada uma cópia de segurança no armazenamento **privado** da aplicação (inacessível a outras apps), mantendo apenas as 3 mais recentes.
- Os dados da aplicação são excluídos das cópias de segurança automáticas do Android e da transferência entre dispositivos; para mudar de telemóvel, exporte e restaure um backup.

## 5. Eliminação de Dados
- Pode eliminar todos os seus dados a qualquer momento acedendo a **Definições > Limpar Dados**.
- A desinstalação da aplicação do seu dispositivo elimina permanentemente toda a base de dados local.

## 6. Contacto
Se tiver alguma dúvida sobre esta política de privacidade, pode contactar-nos através do endereço: `suporte.finflow@gmail.com` (ou o seu e-mail de suporte).
