# Guia de Compliance e Preenchimento — Google Play Console (Finflow)

Este guia contém as respostas exatas para o questionário de **Conteúdo da Aplicação** (*App Content*) na Google Play Console.

---

### 1. Funcionalidades Financeiras (Financial Features Policy)
A Google Play tem regras muito estritas para aplicações de finanças. No formulário de funcionalidades financeiras:
- **Categoria:** *Gestão de Finanças Pessoais (Personal Financial Management / PFM)*.
- **Declaração:** Assinalar que a app **NÃO** concede empréstimos (*personal loans*), **NÃO** opera contas bancárias (*banking services*), **NÃO** executa ordens de bolsa/corretagem (*brokerage/trading*) e **NÃO** faz custódia de fundos ou criptoativos.
- **Justificação (se solicitada):**
  > "Finflow is an offline personal finance tracking application for expense recording, personal budgeting, and wealth simulation. All calculations are performed on-device. The application does not provide loans, banking services, or payment execution."

---

### 2. Segurança dos Dados (Data Safety Form)
No questionário obrigatório de Segurança dos Dados:
- **A sua aplicação recolhe ou partilha algum dos tipos de dados de utilizador obrigatórios?**
  ➡️ **NÃO** (os dados nunca saem do dispositivo local do utilizador para servidores externos).
- **Os dados são encriptados em trânsito?**
  ➡️ Os dados não transitam pela internet. A base de dados local é protegida em repouso por encriptação SQLCipher AES-256.
- **Disponibiliza uma forma de os utilizadores solicitarem a eliminação dos seus dados?**
  ➡️ **SIM** (a app possui botão para apagar todos os dados nas Definições, e desinstalar a app limpa todos os registos).

---

### 3. Classificação de Conteúdo (IARC Content Rating)
No questionário de classificação etária:
- Não contém violência, sangue, linguagem imprópria ou temas adultos.
- Não inclui compras integradas de bens físicos ou jogos de azar com dinheiro real.
- **Resultado esperado:** PEGI 3 / Para Todas as Idades (*Everyone*).

---

### 4. Acesso a Aplicações (App Access)
- Se a app tiver autenticação biométrica ativada:
  ➡️ Selecionar: **Todas as funcionalidades estão disponíveis sem restrições especiais** (uma vez que o bloqueio biométrico é opcional e configurado pelo utilizador após o primeiro arranque).

---

### 5. Anúncios
- A sua aplicação contém anúncios?
  ➡️ **NÃO** (Sem anúncios).

---

### 6. Isenção de Responsabilidade Legal (Disclaimer Financeiro)
A app inclui cálculos de deduções fiscais de PPR (art. 21º do EBF) e taxas de poupança. Para cumprir as normas da CMVM e do Banco de Portugal:
> **Aviso Legal:** *As projeções, cálculos de deduções fiscais de PPR e métricas de património apresentados no Finflow têm caráter estritamente informativo e pedagógico de planeamento pessoal. Não constituem aconselhamento financeiro, fiscal ou de investimento recomendados por entidade financeira regulada.*
