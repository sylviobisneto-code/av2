package com.UERJ.POO3.presentation.ui;

import com.UERJ.POO3.modules.vendas.domain.ItemVenda;
import com.UERJ.POO3.modules.vendas.domain.Venda;
import com.UERJ.POO3.modules.vendas.repository.VendaRepository;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "", layout = MainLayout.class)
@PermitAll
public class MainView extends VerticalLayout {

    private final VendaRepository vendaRepository;

    public MainView(VendaRepository vendaRepository) {
        this.vendaRepository = vendaRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Título Principal
        H2 titulo = new H2("Dashboard Interativo - Resumo Financeiro");
        titulo.getStyle().set("text-align", "center").set("width", "100%");
        add(titulo);

        criarIndicadores();
        criarGraficosEstatisticas();
    }

    private void criarIndicadores() {

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime inicio7Dias = agora.minusDays(7);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<Venda> todasVendas = vendaRepository.findAllComItensEProdutos();

        double totalHoje = 0.0;
        double total7Dias = 0.0;
        double totalMes = 0.0;

        for (Venda venda : todasVendas) {
            if ("CONCLUIDA".equalsIgnoreCase(venda.getStatus()) && venda.getDataVenda() != null) {
                double valor = venda.getValorTotal();
                LocalDateTime data = venda.getDataVenda();

                if (!data.isBefore(inicioHoje)) {
                    totalHoje += valor;
                }
                if (!data.isBefore(inicio7Dias)) {
                    total7Dias += valor;
                }
                if (!data.isBefore(inicioMes)) {
                    totalMes += valor;
                }
            }
        }

        HorizontalLayout indicadores = new HorizontalLayout();
        indicadores.setWidthFull();
        indicadores.setSpacing(true);
        indicadores.setJustifyContentMode(JustifyContentMode.CENTER);

        indicadores.add(
                criarCard("Vendas Hoje", formatarMoeda(totalHoje), "#e8f5e9", "#2e7d32"),
                criarCard("Vendas últimos 7 dias", formatarMoeda(total7Dias), "#e3f2fd", "#1565c0"),
                criarCard("Vendas este Mês", formatarMoeda(totalMes), "#fce4ec", "#8e24aa")
        );

        add(indicadores);
    }

    private VerticalLayout criarCard(String titulo, String valor, String bgColor, String textColor) {

        H4 tituloCard = new H4(titulo);
        tituloCard.getStyle()
                .set("margin", "0")
                .set("font-size", "14px")
                .set("color", "#555")
                .set("font-weight", "500");

        Span valorCard = new Span(valor);
        valorCard.getStyle()
                .set("font-size", "22px")
                .set("font-weight", "bold")
                .set("color", textColor);

        VerticalLayout card = new VerticalLayout(tituloCard, valorCard);
        card.setWidth("30%");
        card.setHeight("110px");
        card.setPadding(true);
        card.setSpacing(false);
        card.setAlignItems(Alignment.CENTER);
        card.setJustifyContentMode(JustifyContentMode.CENTER);

        card.getStyle()
                .set("background-color", bgColor)
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        return card;
    }

    private void criarGraficosEstatisticas() {

        HorizontalLayout painelGraficos = new HorizontalLayout();
        painelGraficos.setWidthFull();
        painelGraficos.setSpacing(true);

        List<Venda> vendas = vendaRepository.findAllComItensEProdutos();

        // 1. Faturamento por Forma de Pagamento
        Map<String, Double> faturamentoPorPagamento = new HashMap<>();
        double totalGeral = 0.0;

        // 2. Top Produtos Mais Vendidos
        Map<String, Integer> produtosQtd = new HashMap<>();

        for (Venda venda : vendas) {
            if ("CONCLUIDA".equalsIgnoreCase(venda.getStatus())) {
                // Pagamento
                String forma = venda.getFormaPagamento() != null ? venda.getFormaPagamento() : "Outros";
                faturamentoPorPagamento.put(forma, faturamentoPorPagamento.getOrDefault(forma, 0.0) + venda.getValorTotal());
                totalGeral += venda.getValorTotal();

                // Produtos
                if (venda.getItens() != null) {
                    for (ItemVenda item : venda.getItens()) {
                        if (item.getProduto() != null) {
                            String nomeProd = item.getProduto().getNome();
                            produtosQtd.put(nomeProd, produtosQtd.getOrDefault(nomeProd, 0) + item.getQuantidade());
                        }
                    }
                }
            }
        }

        // --- Coluna Esquerda: Faturamento por Pagamento ---
        VerticalLayout colPagamento = new VerticalLayout();
        colPagamento.setWidth("50%");
        colPagamento.add(new H3("Faturamento por Pagamento"));

        if (faturamentoPorPagamento.isEmpty()) {
            colPagamento.add(new Span("Nenhum dado registrado."));
        } else {
            final double finalTotalGeral = totalGeral > 0 ? totalGeral : 1.0;
            faturamentoPorPagamento.forEach((forma, valor) -> {
                Span label = new Span(forma + ": " + formatarMoeda(valor));
                label.getStyle().set("font-size", "13px").set("font-weight", "500");

                ProgressBar bar = new ProgressBar();
                bar.setValue(valor / finalTotalGeral);

                VerticalLayout itemLayout = new VerticalLayout(label, bar);
                itemLayout.setPadding(false);
                itemLayout.setSpacing(false);
                colPagamento.add(itemLayout);
            });
        }

        // --- Coluna Direita: Top Produtos ---
        VerticalLayout colProdutos = new VerticalLayout();
        colProdutos.setWidth("50%");
        colProdutos.add(new H3("Top Produtos Mais Vendidos"));

        if (produtosQtd.isEmpty()) {
            colProdutos.add(new Span("Nenhum produto vendido."));
        } else {
            int maxQtd = produtosQtd.values().stream().max(Integer::compare).orElse(1);

            produtosQtd.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .forEach(entry -> {
                        Span label = new Span(entry.getKey() + " (" + entry.getValue() + " unid.)");
                        label.getStyle().set("font-size", "13px").set("font-weight", "500");

                        ProgressBar bar = new ProgressBar();
                        bar.setValue((double) entry.getValue() / maxQtd);

                        VerticalLayout itemLayout = new VerticalLayout(label, bar);
                        itemLayout.setPadding(false);
                        itemLayout.setSpacing(false);
                        colProdutos.add(itemLayout);
                    });
        }

        painelGraficos.add(colPagamento, colProdutos);
        add(painelGraficos);
    }

    private String formatarMoeda(double valor) {
        return String.format(
                Locale.forLanguageTag("pt-BR"),
                "R$ %.2f",
                valor
        );
    }
}