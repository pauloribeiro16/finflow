package com.financas.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankNotificationParserTest {

    private val parser = BankNotificationParser()

    @Test
    fun parseMbwayPayment_extractsAmountAndMerchant() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = "MB WAY",
            text = "Pagou 12,50 € em Continente Bom Dia"
        )
        assertNotNull(result)
        assertEquals(1250L, result?.amountCents)
        assertEquals("Continente Bom Dia", result?.merchant)
        assertEquals("MBWAY", result?.paymentMethod)
        assertEquals("MB WAY", result?.sourceApp)
    }

    @Test
    fun parseMbwayTransfer_extractsRecipient() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = "Envio de dinheiro",
            text = "Enviou 15,00 € para 912345678"
        )
        assertNotNull(result)
        assertEquals(1500L, result?.amountCents)
        assertEquals("912345678", result?.merchant)
        assertEquals("MBWAY", result?.paymentMethod)
    }

    @Test
    fun parseMoeyPayment_extractsAmountAndMerchant() {
        val result = parser.parse(
            packageName = "com.moey",
            title = "Pagamento efetuado",
            text = "Pagamento com sucesso: 18,30 € em Pingo Doce"
        )
        assertNotNull(result)
        assertEquals(1830L, result?.amountCents)
        assertEquals("Pingo Doce", result?.merchant)
        assertEquals("CARD", result?.paymentMethod)
        assertEquals("Moey", result?.sourceApp)
    }

    @Test
    fun parseMoeyMbwayPayment_detectsMbwayMethod() {
        val result = parser.parse(
            packageName = "com.moey",
            title = "Transferência",
            text = "Enviaste 10,00 € via MB WAY para João Silva"
        )
        assertNotNull(result)
        assertEquals(1000L, result?.amountCents)
        assertEquals("João Silva", result?.merchant)
        assertEquals("MBWAY", result?.paymentMethod)
    }

    @Test
    fun parseBankNotification_usesTitleAsMerchant() {
        val result = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Continente",
            text = "12,50 € com o cartão *1234"
        )
        assertNotNull(result)
        assertEquals(1250L, result?.amountCents)
        assertEquals("Continente", result?.merchant)
        assertEquals("CARD", result?.paymentMethod)
        assertEquals("Revolut", result?.sourceApp)
    }

    @Test
    fun parseUnknownApp_withoutExplicitAmount_returnsNull() {
        val unknownResult = parser.parse(
            packageName = "com.unknown.app",
            title = "Aviso",
            text = "Pagamento autorizado 20,50"
        )
        assertNull(unknownResult)

        val financialResult = parser.parse(
            packageName = "com.moey",
            title = "Aviso",
            text = "Pagamento autorizado 20,50"
        )
        assertNotNull(financialResult)
        assertEquals(2050L, financialResult?.amountCents)
    }

    @Test
    fun parseBankCardAuthorization_extractsMerchantAndAmount() {
        val result = parser.parse(
            packageName = "pt.activobank.mobile",
            title = "Autorização de despesa",
            text = "Compra autorizada de 24,99 € no comerciante ZARA"
        )
        assertNotNull(result)
        assertEquals(2499L, result?.amountCents)
        assertEquals("ZARA", result?.merchant)
        assertEquals("CARD", result?.paymentMethod)
    }

    @Test
    fun nonFinancialNotification_returnsNull() {
        val result = parser.parse(
            packageName = "com.whatsapp",
            title = "Amigo",
            text = "Olá, tudo bem? Vemo-nos amanhã!"
        )
        assertNull(result)
    }

    @Test
    fun zeroOrNegativeAmount_returnsNull() {
        val result = parser.parse(
            packageName = "com.moey",
            title = "Alerta",
            text = "Saldo da conta é de 0,00 €"
        )
        assertNull(result)
    }

    @Test
    fun parseEurCurrencyPrefix_extractsCorrectly() {
        val result = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Pagamento",
            text = "Pagou € 45,50 em H&M Lisboa"
        )
        assertNotNull(result)
        assertEquals(4550L, result?.amountCents)
        assertEquals("H&M Lisboa", result?.merchant)
        assertEquals("Revolut", result?.sourceApp)
    }

    @Test
    fun parseWithoutMerchant_fallsBackToUnknown() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = "Pagamento",
            text = "Pagamento de 8,00 € efetuado com sucesso."
        )
        assertNotNull(result)
        assertEquals(800L, result?.amountCents)
        assertTrue(result!!.merchant.startsWith(BankNotificationParser.FALLBACK_EXPENSE_PREFIX))
    }

    @Test
    fun parseWholeEuroAmount_withoutDecimals() {
        val result = parser.parse(
            packageName = "com.sibs.android.mbway",
            title = "MB WAY",
            text = "Pagou 15 € em Pingo Doce"
        )
        assertNotNull(result)
        assertEquals(1500L, result?.amountCents)
        assertEquals("Pingo Doce", result?.merchant)
        assertEquals("MBWAY", result?.paymentMethod)
        assertEquals("MB WAY", result?.sourceApp)
    }

    @Test
    fun parseGluedEuroAmount_withoutDecimals() {
        val result = parser.parse(
            packageName = "com.sibs.android.mbway",
            title = "MB WAY",
            text = "Pagou 15€ em Pingo Doce"
        )
        assertNotNull(result)
        assertEquals(1500L, result?.amountCents)
        assertEquals("Pingo Doce", result?.merchant)
    }

    @Test
    fun parseThousandSeparator_extractsCorrectCents() {
        val result = parser.parse(
            packageName = "pt.bcp.mobile",
            title = "Millennium BCP",
            text = "Compra autorizada de 1.250,50 € na Worten"
        )
        assertNotNull(result)
        assertEquals(125050L, result?.amountCents)
        assertEquals("Worten", result?.merchant)
        assertEquals("Millennium BCP", result?.sourceApp)
    }

    @Test
    fun parseCgdCaixaDirecta_detectsAppAndAmount() {
        val result = parser.parse(
            packageName = "pt.cgd.caixadireta",
            title = "CaixaDirecta",
            text = "Pagamento de 32,80 EUR em Galp"
        )
        assertNotNull(result)
        assertEquals(3280L, result?.amountCents)
        assertEquals("Galp", result?.merchant)
        assertEquals("Caixa Geral de Depósitos", result?.sourceApp)
    }

    @Test
    fun direction_sentMbWayPayment_isExpenseWithMbwayMethod() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = null,
            text = "Enviou 25,00 € para João via MB Way"
        )
        assertNotNull(result)
        assertEquals("EXPENSE", result?.direction)
        assertEquals("MBWAY", result?.paymentMethod)
    }

    @Test
    fun direction_receivedFromPerson_isIncome() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = null,
            text = "Recebeu 50,00 € de Maria por MB Way"
        )
        assertNotNull(result)
        assertEquals(5000L, result?.amountCents)
        assertEquals("INCOME", result?.direction)
    }

    @Test
    fun direction_paymentApprovalText_isExpense() {
        val result = parser.parse(
            packageName = "com.moey",
            title = null,
            text = "Pagamento MB Way aprovado 12,50 € no Continente"
        )
        assertNotNull(result)
        assertEquals(1250L, result?.amountCents)
        assertEquals("EXPENSE", result?.direction)
    }

    @Test
    fun direction_enviasteWholeEuros_isExpense() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = null,
            text = "Enviaste 10 € via MB Way para X"
        )
        assertNotNull(result)
        assertEquals(1000L, result?.amountCents)
        assertEquals("EXPENSE", result?.direction)
    }

    @Test
    fun direction_recebeste_isIncome() {
        val result = parser.parse(
            packageName = "pt.sibs.mbway",
            title = null,
            text = "Recebeste 8,00 €"
        )
        assertNotNull(result)
        assertEquals(800L, result?.amountCents)
        assertEquals("INCOME", result?.direction)
    }

    @Test
    fun direction_textWithoutDirectionWords_defaultsToExpense() {
        val result = parser.parse(
            packageName = "com.moey",
            title = null,
            text = "Movimento de 3,20 € no multibanco"
        )
        assertNotNull(result)
        assertEquals("EXPENSE", result?.direction)
    }

    @Test
    fun direction_mixedKeywords_decidesByFirstOccurrenceInText() {
        val result = parser.parse(
            packageName = "com.moey",
            title = null,
            text = "Recebido aviso de pagamento: 20,00 € em Jumbo"
        )
        assertNotNull(result)
        assertEquals("INCOME", result?.direction)
    }

    @Test
    fun parseXtbDepositNotification_extractsAmountAndBroker() {
        val result = parser.parse(
            packageName = "com.xtb.xstation5",
            title = "Depósito efetuado",
            text = "Depósito de 150,00 € creditado na sua conta XTB"
        )
        assertNotNull(result)
        assertEquals(15000L, result?.amountCents)
        assertEquals("XTB", result?.sourceApp)
        assertEquals("TRANSFER", result?.paymentMethod)
    }

    @Test
    fun parseWiseTransferNotification_extractsAmount() {
        val result = parser.parse(
            packageName = "com.transferwise.android",
            title = "Transferência enviada",
            text = "Enviaste 45,50 € para Pedro Santos"
        )
        assertNotNull(result)
        assertEquals(4550L, result?.amountCents)
        assertEquals("Pedro Santos", result?.merchant)
        assertEquals("Wise", result?.sourceApp)
        assertEquals("TRANSFER", result?.paymentMethod)
    }

    @Test
    fun parseMealCardNotification_extractsAmountAndCardMethod() {
        val result = parser.parse(
            packageName = "com.coverflex.app",
            title = "Compra aprovada",
            text = "Pagamento de 8,90 € em Restaurante O Forno"
        )
        assertNotNull(result)
        assertEquals(890L, result?.amountCents)
        assertEquals("Restaurante O Forno", result?.merchant)
        assertEquals("Cartão Refeição", result?.sourceApp)
    }

    @Test
    fun parseMoeyCreditCardNotification_extractsMerchantAndAmountCorrectly() {
        val result = parser.parse(
            packageName = "pt.moey.app",
            title = "Cartão de Crédito",
            text = "Compra aprovada: 34,90 € em Pingo Doce com o teu cartão de crédito"
        )
        assertNotNull(result)
        assertEquals(3490L, result?.amountCents)
        assertEquals("Pingo Doce", result?.merchant)
        assertEquals("CARD", result?.paymentMethod)
        assertEquals("Moey", result?.sourceApp)
        assertEquals("EXPENSE", result?.direction)
    }

    @Test
    fun parseMoeyCreditCardWithGenericTitle_extractsMerchantFromText() {
        val result = parser.parse(
            packageName = "pt.moey.app",
            title = "moey!",
            text = "Pagamento efetuado com o cartão de crédito: 12,00 € em Restaurante Alentejano"
        )
        assertNotNull(result)
        assertEquals(1200L, result?.amountCents)
        assertEquals("Restaurante Alentejano", result?.merchant)
        assertEquals("CARD", result?.paymentMethod)
        assertEquals("Moey", result?.sourceApp)
    }

    private fun merchantOf(pkg: String, title: String?, text: String?, extra: List<String> = emptyList()): String? =
        parser.parse(packageName = pkg, title = title, text = text, extraLines = extra)?.merchant

    @Test
    fun cgdCardReference_doesNotBecomeMerchant() {
        assertEquals(
            "Continente",
            merchantOf("cgd.pt.caixadirectaparticulares", "Caixadirecta", "Compra de 25,00 EUR no cartão ****1234 em Continente")
        )
    }

    @Test
    fun receivedFromPerson_usesDe() {
        assertEquals("Maria Silva", merchantOf("pt.sibs.android.mbway", "MB WAY", "Recebeste 10,00 € de Maria Silva"))
    }

    @Test
    fun sentToPerson_usesA() {
        assertEquals("Ana Costa", merchantOf("pt.sibs.android.mbway", "MB WAY", "Enviaste 5,00 € à Ana Costa"))
    }

    @Test
    fun merchantWithAsteriskAndUppercase_isPrettified() {
        assertEquals("Uber *Trip", merchantOf("pt.moey.app", "moey!", "Compra de 7,40 € em UBER *TRIP com cartão de crédito."))
    }

    @Test
    fun uppercaseMerchantWithoutPreposition_isFound() {
        assertEquals("Pingo Doce Lisboa", merchantOf("pt.moey.app", "moey!", "Compra com cartão 12,50 € PINGO DOCE LISBOA"))
    }

    @Test
    fun revolutEnglish_spentAt() {
        assertEquals("Continente", merchantOf("com.revolut.revolut", "Revolut", "You spent €12.50 at Continente"))
    }

    @Test
    fun emojiTitle_isMerchant() {
        assertEquals("Lidl", merchantOf("com.revolut.revolut", "🛒 Lidl", "-12,30 €"))
    }

    @Test
    fun inboxStyleLines_areUsedWhenTextIsGeneric() {
        assertEquals(
            "Farmácia Central",
            merchantOf("pt.moey.app", "moey!", "Nova compra", listOf("Compra de 9,90 € na Farmácia Central"))
        )
    }

    @Test
    fun timeAndDateSuffixes_areRemoved() {
        assertEquals("Galp", merchantOf("cgd.pt.caixadirectaparticulares", "Caixadirecta", "Pagamento de 40,00 EUR em Galp às 14:32"))
        assertEquals("Galp", merchantOf("cgd.pt.caixadirectaparticulares", "Caixadirecta", "Pagamento de 40,00 EUR em Galp em 09/11"))
    }

    @Test
    fun unknownMerchant_usesSourceAppFallback() {
        val merchant = merchantOf("pt.moey.app", "moey!", "Pagamento de 8,00 € efetuado com sucesso.")
        assertEquals("Pagamento Moey", merchant)
        val income = merchantOf("pt.sibs.android.mbway", "MB WAY", "Recebeste 8,00 €")
        assertEquals("Recebimento MB WAY", income)
    }
}
