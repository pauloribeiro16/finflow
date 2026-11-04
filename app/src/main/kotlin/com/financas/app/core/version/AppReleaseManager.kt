package com.financas.app.core.version

enum class ReleaseType(val label: String) {
    MAJOR("Major Release"),
    MINOR("Minor Feature"),
    PATCH("Patch & Fix")
}

data class AppRelease(
    val version: String,
    val buildCode: Int,
    val date: String,
    val type: ReleaseType,
    val title: String,
    val description: String,
    val changes: List<String>
)

object AppReleaseManager {
    const val CURRENT_VERSION = "2.2.2"
    const val CURRENT_BUILD = 18
    const val RELEASE_CHANNEL = "Feature Release"

    val RELEASES: List<AppRelease> = listOf(
        AppRelease(
            version = "2.2.2",
            buildCode = 18,
            date = "04 Nov 2026",
            type = ReleaseType.PATCH,
            title = "Assinatura Fixa & Atualizações Simples",
            description = "A partir desta versão todas as releases são assinadas com a mesma chave, permitindo atualizar diretamente por cima da anterior.",
            changes = listOf(
                "Releases assinadas sempre com a mesma chave: o botão 'Tap to Install' atualiza a app sem perder dados.",
                "Quem tem a v2.2.0 ou v2.2.1 deve exportar um backup (Conta → Sistema), instalar esta versão e importar o backup."
            )
        ),
        AppRelease(
            version = "2.2.1",
            buildCode = 17,
            date = "04 Nov 2026",
            type = ReleaseType.PATCH,
            title = "Consistência Visual & Modo Claro/Escuro",
            description = "Correções de UI, formatação PT-PT e botão de tema disponível em todos os ecrãs.",
            changes = listOf(
                "Botão de modo diurno/noturno disponível em todos os separadores e na Conta.",
                "Percentagens com vírgula decimal em todo o lado e sem '-0,0%'; variação face ao mês anterior calculada a partir dos dados reais.",
                "Conta: nome, foto, idioma e aparência sempre atualizados; cabeçalho e barra inferior coerentes com o ecrã aberto.",
                "Ícones e etiquetas de contexto dos movimentos corretos por categoria; splash com o fundo escuro da app.",
                "Média diária correta em meses passados e correção do erro de lint na lista de movimentos do Início."
            )
        ),
        AppRelease(
            version = "2.2.0",
            buildCode = 16,
            date = "29 Out 2026",
            type = ReleaseType.MINOR,
            title = "Transições Fluidas & Dinâmicas (Material 3 Expressive)",
            description = "Transições modernas de alto nível com física spring e animações táteis inspiradas nos melhores softwares atuais.",
            changes = listOf(
                "Navegação com transições direccionais: deslize fluido (slide + fade + scale) entre Início, Movimentos, Análise, Orçamentos e Conta.",
                "Barra inferior táctil e reactiva: pílula activa animada, salto elástico no ícone e botão 'Nova' flutuante em gradiente com feedback de toque.",
                "Sub-navegação de Análise fluida: troca entre lentes (Visão Anual, No Quê, Onde, Como, Evolução, Comparar, Calendário) com animações horizontais suaves.",
                "Seletor temporal estilo odómetro: animação vertical de flip de calendário ao navegar entre meses (< >).",
                "Micro-interacções em filtros: pílulas de filtro e valores de sumário interpolam de forma suave e instantânea."
            )
        ),
        AppRelease(
            version = "2.1.1",
            buildCode = 15,
            date = "29 Out 2026",
            type = ReleaseType.PATCH,
            title = "Sincronização de Meses & Barra de Movimentos",
            description = "Correção da ancoragem de mês em Movimentos e integração de navegação completa de 5 pontos.",
            changes = listOf(
                "Sincronização bidirecional: o separador de Movimentos agora abre e ancora no mês visualizado no Início.",
                "Seletor de Mês no cabeçalho de Movimentos com navegação rápida (< >) e diálogo para qualquer mês/ano (2024 a 2027).",
                "Nova Barra de Navegação de 5 Pontos com acesso direto aos Movimentos.",
                "Expansão in-place no Início: botão 'Mostrar mais / menos' e filtros 'Todas', 'Hoje' e 'Ontem' reativos."
            )
        ),
        AppRelease(
            version = "2.1.0",
            buildCode = 14,
            date = "28 Out 2026",
            type = ReleaseType.MINOR,
            title = "Visão Anual 360° & Dados Discriminados",
            description = "Análise aprofundada multi-ano com movimentos discriminados por subcategoria.",
            changes = listOf(
                "Nova lente Anual no ecrã de Análise com seletor de anos (2024, 2025, 2026) e gráfico interativo de 12 meses.",
                "Discriminação completa de transações: cada subcategoria agora expande e lista individualmente todas as compras.",
                "Motor de importação resiliente para formatos irregulares e imprevisíveis sem perda de dados."
            )
        ),
        AppRelease(
            version = "2.0.0",
            buildCode = 10,
            date = "28 Out 2026",
            type = ReleaseType.MAJOR,
            title = "Redesign Finflow (Stitch Modern Wealth)",
            description = "Transformação visual completa com base no design system Stitch Finflow.",
            changes = listOf(
                "Interface limpa em fundo claro e destaques índigo vibrante e verde esmeralda.",
                "Novo Card de Despesas de Alto Impacto com indicador de teto e meta.",
                "Indicadores de Média Diária, Ritmo Sustentável e Ritmo Semanal (4 semanas)."
            )
        ),
        AppRelease(
            version = "1.2.0",
            buildCode = 5,
            date = "27 Out 2026",
            type = ReleaseType.MINOR,
            title = "Orçamentos e Metas de Poupança",
            description = "Definição de tetos mensais por categoria e metas de poupança dedicadas.",
            changes = listOf(
                "Ecrã de Orçamentos com barra de progresso em tempo real.",
                "Metas financeiras com prazo e progresso percentual."
            )
        ),
        AppRelease(
            version = "1.1.0",
            buildCode = 3,
            date = "26 Out 2026",
            type = ReleaseType.MINOR,
            title = "Calculadora Numpad & Sugestões Rápidas",
            description = "Entrada rápida com cálculos matemáticos integrados e pré-preenchimento.",
            changes = listOf(
                "Teclado numérico com operações aritméticas (+, -, *, /) diretamente no montante.",
                "Preenchimento automático inteligente de contexto e método de pagamento."
            )
        ),
        AppRelease(
            version = "1.0.0",
            buildCode = 1,
            date = "25 Out 2026",
            type = ReleaseType.MAJOR,
            title = "Lançamento Inicial Offline-First",
            description = "Arquitetura financeira com SQLite Room e matemática monetária segura em cêntimos.",
            changes = listOf(
                "Base de dados SQLite Room 100% offline no dispositivo sem telemetria.",
                "Valores monetários guardados em cêntimos (Long) sem erros de arredondamento.",
                "Categorias e subcategorias predefinidas em PT-PT."
            )
        )
    )

    fun latestRelease(): AppRelease = RELEASES.first()
}
