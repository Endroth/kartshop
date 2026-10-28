# Kart Shop / SimpleShopGUI — código-fonte

Plugin de loja para Paper/Spigot 1.21.x com Vault e um plugin de economia.

## Recursos
- Arco sem encantamentos por $80.
- Besta sem encantamentos por $80.
- Categoria `ENCANTAMENTOS` no menu principal com quatro livros: Durabilidade III, Remendo, Recarga Rápida III e Chama I.
- Compra de itens e aplicação de encantamentos configurados no `config.yml`.
- Comando para adicionar à loja o item que o administrador está segurando.
- Mantém as demais lojas existentes no `resources/config.yml`.

## Comandos
- `/shop` — abre o menu principal.
- `/shop <categoria>` — abre uma loja pelo nome configurado, por exemplo `/shop pvp`.
- `/shop add <categoria> <preço>` — adiciona à loja o item da mão principal, salvando material, quantidade e encantamentos normais. Exemplo: segure uma espada e use `/shop add pvp 250`.
- `/shop reload` — recarrega o `config.yml`.
- `/shop help` — mostra os comandos.

`/shop add` requer a permissão `simpleshop.admin` (padrão: OP). `/shop reload` requer `simpleshop.reload` (padrão: OP). A categoria usada no comando precisa já existir em `shops:` no `config.yml`.

Os itens novos são colocados automaticamente no primeiro espaço livre entre os slots 9–17; se a página estiver cheia, o comando cria uma nova página automaticamente.

## Compilar
Use Java 21 e Maven instalado, dentro desta pasta:

```bash
mvn clean package
```

O arquivo será criado em `target/SimpleShopGUI.jar`.

## Instalar
1. Faça backup do plugin e da pasta `plugins/SimpleShop/` atual.
2. Compile o projeto e copie `target/SimpleShopGUI.jar` para a pasta `plugins` do servidor.
3. Se o servidor já tem `plugins/SimpleShop/config.yml`, faça backup e atualize com `resources/config.yml` para receber a categoria de encantamentos e as configurações atualizadas.
4. Reinicie o servidor. É necessário Vault e um plugin de economia compatível.

Os preços dos quatro livros podem ser alterados em `resources/config.yml`.
