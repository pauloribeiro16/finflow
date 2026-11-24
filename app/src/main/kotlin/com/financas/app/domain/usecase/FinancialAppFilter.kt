package com.financas.app.domain.usecase

object FinancialAppFilter {

    val ALLOWED_PACKAGES: Set<String> = setOf(
        "pt.sibs.android.mbway",
        "pt.sibs.mbway",
        "com.sibs.android.mbway",
        "com.moey",
        "com.moey.android",
        "pt.moey.app",
        "com.paypal.android.p2pmobile",
        "com.revolut.revolut",
        "com.revolut.revolut.test",
        "pt.cgd.caixadirecta",
        "pt.cgd.caixadireta",
        "pt.bcp.mobile",
        "wit.android.bcpbankingapp.millennium",
        "com.santander.app",
        "pt.santandertotta.mobileparticulares",
        "com.activobank.mobile",
        "pt.activobank.mobile",
        "pt.novobanco.mobile",
        "pt.novobanco.app",
        "pt.montepio.geral",
        "pt.montepio.app",
        "com.bankinter.app",
        "pt.bankinter.mobile",
        "pt.bpi.mobile",
        "pt.bancobpi.bpiapp",
        "pt.bancobpi.mobile.fiabilizacao",
        "pt.bancoctt.app",
        "pt.creditoagricola.app",
        "pt.bancobest.app",
        "pt.abanca.app",
        "com.xtb.xstation2",
        "com.xtb.xstation5",
        "de.traderepublic.app",
        "nl.degiro.trader",
        "com.transferwise.android",
        "de.number26.android",
        "com.imaginecurve.curve.prd",
        "com.openbank.es",
        "com.coverflex.app",
        "com.edenred.myedenred",
        "com.pluxee.app",
        "com.wizink.app",
        "pt.unicre.unibanco",
        "pt.cetelem.app",
        "pt.cofidis.app"
    )

    fun isFinancialApp(packageName: String): Boolean {
        val normalized = packageName.trim().lowercase()
        return normalized.isNotEmpty() && normalized in ALLOWED_PACKAGES
    }
}
