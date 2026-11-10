# Plano técnico em 10 fases para o Vertical Trapdoors

## Sequência de entrega

A **fase 1 migra o NeoForge 1.21.1 para a nova estrutura** e valida a fundação usando o mod que já existe. Cada fase seguinte adiciona exatamente uma combinação de Minecraft e loader, entrega seu JAR de produção e mantém os alvos anteriores passando no CI. O trabalho entra na `main` em um único PR. A entrega intermediária é o artefato do CI associado ao commit da fase, sem bump ou tag individual.

| Fase | Entrega | Java | Gradle Wrapper |
|---|---|---:|---:|
| 1 | Fundação e NeoForge 1.21.1 | 21 | 8.11.1 |
| 2 | Fabric 1.21.1 | 21 | 8.11.1 |
| 3 | NeoForge 26.1.2 | 25 | 9.4.0 |
| 4 | Fabric 26.1.2 | 25 | 9.4.0 |
| 5 | Forge 1.20.1 | 17 | 8.11.1 |
| 6 | Fabric 1.20.1 | 17 | 8.11.1 |
| 7 | NeoForge 26.2 | 25 | 9.5.1 |
| 8 | Fabric 26.2 | 25 | 9.5.1 |
| 9 | NeoForge 26.3 | 25 | 9.6.0 |
| 10 | Fabric 26.3 e fechamento da matriz | 25 | 9.6.0 |

A matriz usa versões exatas do Minecraft. A linha 26.1 começa em **26.1.2**, seu último patch estável. **26.4 fica fora do escopo.** Os números de dependências abaixo são pins iniciais para a implementação. A resolução com cache limpo e o teste do JAR são os gates que confirmam cada combinação. [Fabric Meta](https://meta.fabricmc.net/v2/versions/game) · [Fabric 26.1](https://www.fabricmc.net/2026/03/14/261.html) · [Fabric 26.2](https://fabricmc.net/2026/06/15/262.html) · [Fabric 26.3](https://fabricmc.net/2026/09/15/263.html)

## Estrutura e contrato entre as fases

```text
gradle.properties                        identidade e versão únicas do mod
gradle/target-conventions.gradle         convenções de build e empacotamento
shared/src/main/java/                    orientação, propriedades e estado de Alt
shared/src/client/java/                  transformação do painel e sincronização local
targets/<minecraft>/                     settings, propriedades e Wrapper próprios
targets/<minecraft>/src/main/java/       Mixins vanilla compartilhados pelos dois loaders
targets/<minecraft>/<loader>/            build, entrada, rede, cliente, metadata e testes
.github/targets.json                     matriz cumulativa de alvos ativos
tools/check_jar.py                        verificação do artefato de produção
```

Os nomes de classes abaixo usam o prefixo `com.radaeli.verticaltrapdoors`. Cada loader compila as fontes compartilhadas contra o próprio Minecraft. As fontes comuns devem continuar compatíveis com Java 17. Um alvo novo reutiliza um diretório de compatibilidade quando as assinaturas e os testes confirmam a compatibilidade. Quando um hook divergir, o alvo seleciona apenas a implementação correspondente ao seu descritor. Cada classe tem uma única origem por compilação.

O contrato de jogo é o atual. `verticaltrapdoors`, `vertical` e `reversed` permanecem estáveis. Em parede, Shift abre da esquerda para a direita, Alt abre no sentido oposto e Shift tem prioridade. Topo, fundo e colocação sem modificador seguem o comportamento vanilla. O painel usa o modelo da trapdoor original, inclusive de outro namespace quando a classe segue o contrato de `TrapDoorBlock`. A colocação é autoritativa no servidor, com previsão local equivalente. O mod é obrigatório nos dois lados.

Todo alvo segue a mesma saída. De `targets/<minecraft>/`, `./gradlew :<loader>:clean :<loader>:build --no-daemon` deve gerar o JAR final em `<loader>/build/libs/verticaltrapdoors-<mod_version>+<minecraft>-<loader>.jar`. No Windows, usar `gradlew.bat` com o JDK da linha. `build` deve executar o empacotamento de produção do loader e os testes configurados. CI publica o JAR, seu SHA-256 e os relatórios de teste por alvo.

## Implementação por fase

### Fase 1 — Fundação e NeoForge 1.21.1

1. **Registrar a referência antes da extração.** Rodar `clean build` na estrutura atual com Java 21 e NeoForge `21.1.236`. Guardar o resultado dos sete testes existentes e preparar uma cópia de mundo com painéis de `0.1.0`, nas duas orientações e em estados aberto e fechado. Preservar a edição local já presente no `README.md`.
2. **Mover o build existente.** Criar `targets/1.21.1/settings.gradle`, `gradle.properties`, Wrapper e `neoforge/build.gradle`. Manter ModDevGradle `2.0.144`. Mover o metadata, `pack.mcmeta`, a configuração Mixin e o teste `TrapDoorModelRegressionTest` para a nova estrutura. Em `gradle/target-conventions.gradle`, centralizar a leitura da identidade, toolchain, diretórios compartilhados, expansão de metadata e nome do artefato. Deixar dependências, remapeamento e configuração de runs nos subprojetos.
3. **Extrair as regras e a geometria.** Mover `TrapDoorProperties` e `TrapDoorOrientation` para `shared/src/main/java`. Mover o cálculo da forma para `TrapDoorOrientation.panelShape(...)` e extrair a matriz para `shared/src/client/java/.../client/PanelTransform.java`. Mover `TrapDoorBlockMixin` e `BlockDefaultStateInvoker` para a raiz de fontes do alvo. Manter as injeções obrigatórias de definição de estado, defaults, colocação e forma. Remover a injeção de dobradiça desativada e proteger os acessos às propriedades em subclasses que não as tenham registrado.
4. **Separar Alt local e remoto.** Extrair `AltModifierState` para `shared`, com estado de previsão local e estado do servidor por jogador. Criar `client/AltInputSync.java` para atualizar a previsão e enviar apenas mudanças. No NeoForge, manter `AltModifierPayload`, `ModNetworking` e o protocolo `1` com ID `verticaltrapdoors:alt_modifier`. O handler atualiza apenas o jogador remetente na thread do servidor. Criar `client/mixin/BlockUseMixin.java` no `HEAD` de `MultiPlayerGameMode#useItemOn`, antes da previsão e do pacote vanilla. Manter a amostragem no tick de cliente para soltar Alt, detectar perda de foco e limpar a sessão. O logout do servidor limpa o estado daquele jogador.
5. **Preservar o bake existente.** Mover `TrapDoorModelEvents`, `TrapDoorRegistrySnapshot` e `RotatedTrapDoorBakedModel` para o subprojeto NeoForge. O wrapper usa `PanelTransform` e continua delegando os dados contextuais e o render type ao modelo de origem. Resolver os modelos de origem a partir de uma cópia do mapa anterior às substituições. Mover as classes cliente e seus Mixins para o conjunto carregado apenas no cliente.
6. **Preparar o CI desde o primeiro JAR.** Criar `.github/targets.json` com a entrada `1.21.1/neoforge` e atualizar `pr-checks.yml` e `release-on-tag.yml` para consumir a matriz. Criar `tools/check_jar.py` com entradas de caminho do JAR, Minecraft, loader, Java e versão do mod. Validar nome, metadata expandido, classes de entrada e Mixins declarados, bytecode e ausência de classes de outros loaders ou dependências empacotadas. O job de release lê a matriz no próprio ref da tag e agrega os JARs depois que todos os jobs passam. Preservar o disparo existente do `auto-tag.yml`.

**Aceite e entrega.** O novo caminho gera um JAR NeoForge 1.21.1. Os sete testes passam sob o ModDevGradle e comprovam que o Mixin de produção foi aplicado. O JAR abre no cliente e servidor dedicado, passa no teste de Alt com clique no mesmo tick e reabre a cópia do mundo antigo mantendo estado, textura e colisão dos painéis.

### Fase 2 — Fabric 1.21.1

1. Criar `targets/1.21.1/fabric/build.gradle`, `src/main/resources/fabric.mod.json` e `verticaltrapdoors.fabric.mixins.json`. Adicionar Fabric Loom `1.9.2`, Loader `0.17.2` e Fabric API `0.116.17+1.21.1` como pins iniciais. Usar mappings oficiais, `splitEnvironmentSourceSets()` e `remapJar`. Incluir os mesmos Mixins vanilla da fase 1.
2. Criar `fabric/VerticalTrapdoorsFabric`, `fabric/client/VerticalTrapdoorsFabricClient`, `fabric/network/FabricAltNetworking` e `FabricCompatibilityHandshake`. Registrar o payload tipado de Alt no canal de jogo e validar a versão de protocolo na fase de configuração. O servidor rejeita um cliente que não conclua a negociação. O cliente verifica a negociação antes de habilitar uso e previsão e encerra a conexão com mensagem clara quando o servidor não tem o mod.
3. Criar `fabric/client/AltModifierClient` e `fabric/client/mixin/BlockUseMixin`. Conectar tick, desconexão e uso do bloco ao fluxo de `AltInputSync`. Registrar callbacks de cliente no entrypoint cliente.
4. Criar `fabric/client/mixin/ModelManagerMixin`, `fabric/client/TrapDoorModelInstaller` e `RotatedFabricBakedModel`. Interceptar o mapa de modelos completos em `ModelManager#loadModels`, entre o bake e a construção do cache de `BlockState`. Envolver apenas as variantes verticais. Implementar a emissão via Fabric Renderer API e a consulta convencional de quads. Preservar o estado de origem, UVs, tint, materiais e normais. Retirar o culling das faces movidas para dentro do bloco e garantir que a transformação é removida do contexto após a emissão.
5. Adicionar `1.21.1/fabric` à matriz, criar as tasks `Vertical Trapdoors: Build Fabric` e `Vertical Trapdoors: Run Fabric Client` em `.vscode/tasks.json`, e verificar o JAR remapeado em uma instalação Fabric fora do ambiente de desenvolvimento. Manter as tasks dos alvos já entregues. [Fabric API 1.21.1](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/) · [Fabric Renderer API 1.21.1](https://raw.githubusercontent.com/FabricMC/fabric/1.21.1/fabric-renderer-api-v1/src/client/java/net/fabricmc/fabric/api/renderer/v1/model/FabricBakedModel.java)

**Aceite e entrega.** Dois alvos verdes. O JAR Fabric conecta a um dedicado compatível, rejeita os dois sentidos de conexão com uma instalação sem o mod e renderiza após `F3+T`, incluindo um modelo que emite quads pela API do Fabric.

### Fase 3 — NeoForge 26.1.2

1. Criar `targets/26.1.2/` com Wrapper e módulo `neoforge`. Usar ModDevGradle `2.0.148`, NeoForge `26.1.2.124`, Java 25 e classes oficiais do jogo. Criar metadata com versão exata de Minecraft e ajustar `pack.mcmeta` ao formato do cliente dessa versão.
2. Criar os Mixins de jogo da linha moderna em `targets/26.1.2/src/main/java`. Confirmar o construtor de `TrapDoorBlock`, definição de estado, colocação, forma e invoker nas fontes resolvidas. Portar a entrada NeoForge, `AltModifierPayload`, `ModNetworking`, `AltModifierClient` e `BlockUseMixin`, incluindo a API de identificadores da versão. Manter o pacote booleano e a sincronização antes do uso.
3. Criar `client/TrapDoorModelEvents`, `client/RotatedTrapDoorBlockStateModel` e `client/RotatedBlockStateModelPart`. Em `ModelEvent.ModifyBakingResult`, resolver o mapa por `BlockState`, selecionar o estado original e envolver suas partes. Transformar posição e direção de cada quad sem modificar a fonte. Preservar o material baked, tint, iluminação, camada e partícula, e passar o contexto de mundo/modelo nas chamadas que o recebem. O cálculo geométrico continua em `PanelTransform`.
4. Portar o teste de regressão do modelo para o pipeline moderno, cobrindo geometria, resolução do estado de origem e material. Adicionar `26.1.2/neoforge` à matriz. [MDK NeoForge 26.1.2](https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle) · [Artefatos NeoForge](https://maven.neoforged.net/releases/net/neoforged/neoforge/) · [Pipeline de modelos 26.1](https://docs.neoforged.net/primer/docs/26.1/)

**Aceite e entrega.** Três alvos verdes. O JAR NeoForge 26.1.2 renderiza a geometria moderna em chunk, exibe partículas corretas e mantém alinhamento, colisão e textura após reload e reinício.

### Fase 4 — Fabric 26.1.2

1. Criar o módulo `targets/26.1.2/fabric` com `build.gradle`, `fabric.mod.json` e configuração Mixin. Usar Loom `1.15.5` no modo sem remapeamento, Loader `0.20.1` e Fabric API `0.155.3+26.1.2`. A saída de produção vem de `jar`.
2. Reutilizar os Mixins vanilla da fase 3. Conectar os entrypoints Fabric, `FabricAltNetworking`, `FabricCompatibilityHandshake`, `AltModifierClient` e `BlockUseMixin` às assinaturas modernas. Manter a validação bilateral de protocolo.
3. Criar `fabric/client/mixin/ModelBakeryMixin`, `fabric/client/TrapDoorModelInstaller` e `RotatedFabricBlockStateModel`. Aplicar o wrapper ao resultado completo do bake, após as modificações por modelo do Fabric e antes da publicação do cache. Transformar a emissão contextual via `emitQuads`, além do caminho que coleta partes vanilla. Delegar partículas e propriedades de material ao modelo de origem. Adicionar `26.1.2/fabric` à matriz. [Loom sem remapeamento](https://docs.fabricmc.net/develop/loom/) · [Wrapper de modelo no Fabric 26.1.2](https://maven.fabricmc.net/docs/fabric-api-0.146.1%2B26.1.2/net/fabricmc/fabric/api/client/model/loading/v1/wrapper/WrapperBlockStateModel.html)

**Aceite e entrega.** Quatro alvos verdes. O JAR Fabric é carregado diretamente em produção e passa nos testes de emissão contextual, reload, rede e persistência.

### Fase 5 — Forge 1.20.1

1. Criar `targets/1.20.1/` com módulo `forge`, Java 17, Forge `47.4.28`, ForgeGradle `6.0.54` e MixinGradle `0.7.38` como pins iniciais. Criar `META-INF/mods.toml`, `pack.mcmeta` e `verticaltrapdoors.forge.mixins.json`. Configurar geração de refmap, registro do Mixin no manifesto e `reobfJar` no caminho de `build`.
2. Criar `TrapDoorBlockMixin` e `BlockDefaultStateInvoker` na raiz do alvo, usando os descritores de 1.20.1. Compilar `shared` em Java 17 e ajustar apenas as chamadas que dependam de símbolos mais novos.
3. Criar `forge/VerticalTrapdoorsForge`, `forge/network/ForgeAltNetworking` e `AltModifierMessage`. Registrar um `SimpleChannel` obrigatório, com versão de protocolo `1`, mensagem booleana C2S e atualização na thread do servidor. Criar `forge/client/AltModifierClient` e `BlockUseMixin` com as assinaturas legadas. Ligar o logout ao estado compartilhado.
4. Criar `forge/client/TrapDoorModelEvents` e `RotatedForgeBakedModel` com `ModelEvent.ModifyBakingResult` e os tipos Forge 1.20.1. Reaproveitar `PanelTransform`, preservando `ModelData`, render type e a coleta de faces. Adicionar `1.20.1/forge` à matriz e verificar especificamente `MixinConfigs` e o refmap no JAR final. [Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) · [Configuração de Mixins no Forge](https://github.com/SpongePowered/Mixin/wiki/Mixins-on-Minecraft-Forge)

**Aceite e entrega.** Cinco alvos verdes. O JAR reobfuscado inicia em Forge 1.20.1 fora do Gradle. Os Mixins aplicam em cliente e dedicado, e a rede e a renderização passam no Java 17.

### Fase 6 — Fabric 1.20.1

1. Criar `targets/1.20.1/fabric` com Loom `1.9.2`, Loader `0.17.2`, Fabric API `0.92.6+1.20.1`, metadata próprio e `remapJar`. Reutilizar os Mixins vanilla da fase 5.
2. Adaptar os entrypoints, `AltModifierClient` e `BlockUseMixin` do Fabric ao alvo. Implementar `FabricAltNetworking` com `FriendlyByteBuf` e canal de jogo legado. Ler o booleano antes de agendar a atualização no servidor. Implementar `FabricCompatibilityHandshake` com query e resposta de login, incluindo a confirmação de servidor no cliente antes de habilitar a colocação.
3. Adaptar `ModelManagerMixin`, `TrapDoorModelInstaller` e `RotatedFabricBakedModel` à chave de modelo e ao caminho de emissão de 1.20.1. Transformar os quads antes da filtragem por face e preservar o contexto fornecido pelo modelo modded. Adicionar `1.20.1/fabric` à matriz. [Fabric API 1.20.1](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.6%2B1.20.1/) · [Negociação no login](https://maven.fabricmc.net/docs/fabric-api-0.92.5%2B1.20.1/net/fabricmc/fabric/api/client/networking/v1/ClientLoginNetworking.html)

**Aceite e entrega.** Seis alvos verdes. O JAR remapeado inicia em Fabric 1.20.1, passa na negociação legada em ambos os sentidos e renderiza com o pipeline de cliente da versão.

### Fase 7 — NeoForge 26.2

1. Criar `targets/26.2/` e seu módulo `neoforge`, com ModDevGradle `2.0.148` e NeoForge `26.2.0.93`. Criar metadata e pack metadata próprios. Reutilizar as fontes modernas de jogo, rede e entrada com assinaturas compatíveis.
2. Portar `TrapDoorModelEvents`, `RotatedTrapDoorBlockStateModel` e `RotatedBlockStateModelPart` contra o resultado de bake e os materiais de 26.2. Manter o modelo de origem já resolvido pelo reload atual, com seus sprites e materiais. Adaptar os testes modernos e adicionar `26.2/neoforge` à matriz. [MDK NeoForge 26.2](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle) · [Migração 26.1.x → 26.2](https://docs.neoforged.net/primer/docs/26.2/)

**Aceite e entrega.** Sete alvos verdes. O JAR 26.2 passa com pack de texturas substituído e recarregado, mantendo materiais, partículas e faces visíveis.

### Fase 8 — Fabric 26.2

1. Criar `targets/26.2/fabric`, com Loom `1.17.7`, Loader `0.20.1`, Fabric API `0.161.0+26.2`, metadata e saída `jar`. Reaplicar os entrypoints e a negociação da fase 4 à API resolvida.
2. Portar `ModelBakeryMixin` e `RotatedFabricBlockStateModel`. Validar o ponto de publicação do cache e as assinaturas da emissão contextual e da coleta de partes. Adicionar `26.2/fabric` à matriz. [Fabric 26.2](https://fabricmc.net/2026/06/15/262.html) · [Artefatos da Fabric API](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/)

**Aceite e entrega.** Oito alvos verdes. O JAR mantém o modelo modded e o recurso substituído após `F3+T`. A reconexão restaura o estado inicial de Alt e a negociação de protocolo continua obrigatória.

### Fase 9 — NeoForge 26.3

1. Criar `targets/26.3/` e seu módulo `neoforge`, com ModDevGradle `2.0.148` e NeoForge `26.3.0.83`. Criar os arquivos de metadata e revisar os descritores dos Mixins de jogo, uso e forma.
2. Criar `targets/26.3/src/client/java/.../client/SdlAltKeys.java` para leitura física de Alt esquerdo e direito via `InputConstants` com os códigos SDL. Incluir esse diretório no cliente NeoForge e substituir a leitura GLFW em `AltModifierClient`. Manter a regra de foco, tela e sessão.
3. Portar `TrapDoorModelEvents` e os wrappers modernos para o resultado de bake de 26.3. Preservar flags de material e a classificação de quads usada pelo renderer. Adicionar `26.3/neoforge` à matriz. [Migração 26.2 → 26.3](https://docs.neoforged.net/primer/docs/26.3/) · [Evento de bake do NeoForge 26.3](https://raw.githubusercontent.com/neoforged/NeoForge/26.3.x/src/client/java/net/neoforged/neoforge/client/event/ModelEvent.java)

**Aceite e entrega.** Nove alvos verdes. Alt esquerdo, Alt direito, perda de foco e clique no mesmo tick passam com SDL. O teste visual inclui modelo recortado e modelo translúcido para conferir a classificação dos materiais.

### Fase 10 — Fabric 26.3 e fechamento

1. Criar `targets/26.3/fabric`, com Loom `1.17.7`, Loader `0.20.1`, Fabric API `0.162.0+26.3`, metadata e saída `jar`. Reutilizar `SdlAltKeys`, portar o `BlockUseMixin` cliente e manter o fluxo de configuração da fase 4.
2. Portar `ModelBakeryMixin` e `RotatedFabricBlockStateModel` às assinaturas 26.3. Preservar a emissão de modelos contextuais, a classificação de materiais, os modelos de partículas e o resultado após recarregar os recursos. Adicionar `26.3/fabric` à matriz. [Fabric 26.3](https://fabricmc.net/2026/09/15/263.html) · [Wrapper de modelo no Fabric 26.3](https://raw.githubusercontent.com/FabricMC/fabric/26.3/fabric-model-loading-api-v1/src/client/java/net/fabricmc/fabric/api/client/model/loading/v1/wrapper/WrapperBlockStateModel.java)
3. Rodar a matriz completa a partir de um checkout limpo. Validar a coleta dos dez JARs usando a mesma lógica do workflow de tag até a etapa anterior à publicação. Confirmar unicidade de nomes, SHA-256 e correspondência entre metadata e matriz. Atualizar os requisitos públicos do README em inglês e sincronizar a matriz no `mod-docs`, preservando a edição local do README.

**Aceite e entrega.** Dez alvos verdes, dez JARs instaláveis, validação de runtime registrada para cada combinação e um PR com a matriz completa. A validação do workflow termina com os artefatos de CI revisáveis. A publicação pública segue a automação existente quando a matriz aprovada entra na `main`.

## Gate obrigatório em todas as fases

- **Tasks do VS Code.** Sempre que uma fase adicionar uma combinação de jogo e loader e entregar um novo JAR, atualizar `.vscode/tasks.json` com a task de build/empacotamento daquele alvo e, quando disponível, a task para iniciar seu cliente. Conferir o wrapper, diretório de trabalho e tarefas Gradle, manter as tasks dos alvos anteriores e registrar essa atualização no fechamento da fase.
- **Build e pacote.** Rodar o build limpo do alvo da fase e o CI cumulativo. Verificar o JAR final, versão de Java, loader, dependências, configuração Mixin e empacotamento de produção. Um build verde apenas no ambiente de desenvolvimento fica pendente até o JAR passar em instalação independente.
- **Cliente, servidor e save.** Usar o JAR produzido em instâncias isoladas. Validar single-player e dedicado, salvar, reiniciar e verificar as propriedades com um painel de cada sentido. Registrar a versão exata do jogo, loader e JAR usados.
- **Colocação e renderização.** Cobrir quatro paredes, clique alto e baixo, Shift, Alt, ambos, nenhum, topo e fundo. Abrir manualmente e por redstone. Conferir colisão, orientação da textura, faces visíveis junto à parede, um modelo de outro namespace e `F3+T`. Usar uma trapdoor compatível de outro mod. Quando o alvo não tiver uma disponível, usar uma fixture separada que registre `third_party:custom_trapdoor`, com geometria e textura de teste, fora do JAR de produção.
- **Rede.** Testar dois jogadores no mesmo dedicado, Alt e clique no mesmo tick, soltura, perda de foco, abertura de menu e reconexão. Confirmar que o Alt de um jogador não afeta o outro e que um cliente ou servidor incompatível recebe uma rejeição clara.
- **Fechamento.** Associar ao commit da fase o link do job, JAR, hash, relatórios e resultado dos testes manuais. Uma falha de Mixin, renderização, persistência ou rede bloqueia o fechamento do alvo e a próxima fase, mesmo com compilação verde.
