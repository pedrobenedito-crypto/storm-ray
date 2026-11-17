# Storm Ray — mod para Minecraft Forge 1.20.1

Uma arraia colossal de cinzas é invocada por um ritual de redstone. Ela tinge o céu de vermelho, traz uma tempestade de raios e petrifica tudo o que estiver vivo embaixo dela. O mod também traz o Reflexo de Cinzas (uma dimensão espelhada), o Coliseu em Ruínas e as Ilhas do Céu.

| | |
|---|---|
| Minecraft | Java Edition **1.20.1** |
| Loader | **Forge 47.x** (projeto configurado com 47.3.0) |
| Dependência | **GeckoLib 4.4.x** para Forge 1.20.1 (obrigatória) |
| Java | 17 |

---

## 1. Instalar e jogar

1. Instale o **Forge 1.20.1** (47.3.0 ou mais recente).
2. Baixe o **GeckoLib** para *Forge 1.20.1* (versão 4.4.x) no CurseForge ou Modrinth.
3. Coloque `geckolib-forge-1.20.1-4.4.x.jar` e `stormray-1.20.1-1.6.0.jar` na pasta `mods` do Minecraft.
4. Abra o jogo com o perfil do Forge.

Em servidor, os dois `.jar` vão na pasta `mods` do servidor **e** de cada jogador.

---

## 2. Como invocar a Storm Ray

Monte um **T de Blocos de Redstone**, igual ao Golem de Ferro:

```
        [Cabeça do Steve]      <- colocada por último
 [Redstone][Redstone][Redstone]
           [Redstone]
```

- 3 blocos de redstone na horizontal (no eixo X ou no Z);
- 1 bloco de redstone embaixo do bloco do meio;
- **por último**, uma **cabeça do Steve** em cima do bloco do meio.

Ao colocar a cabeça, a estrutura é consumida, a terra se abre e a criatura emerge em cerca de 7 segundos (ela fica invulnerável durante a invocação). Cada estrutura só invoca uma vez. Por padrão, um novo ritual falha se já existir uma Storm Ray viva a menos de 256 blocos.

### Cabeça do Steve no survival

No vanilla não existe cabeça de jogador no survival, então o mod adiciona uma receita sem forma:

**Bloco de Osso + Couro + Corante Marrom + Corante Ciano → Cabeça de Jogador (Steve)**

Uma cabeça sem dono é a cabeça do Steve. Cabeças com a skin de outro jogador não funcionam, a não ser que `requireSteveHead = false` na config.

Para testes no criativo também existem o **Ovo Gerador de Arraia da Tempestade** o **Ovo Gerador de Phantom X**, o **Ovo Gerador de Phantom Sea** e o **Ovo Gerador de Phantom Bat**.

---

## 3. O que acontece na luta

- **Céu vermelho:** os jogadores dentro do raio do evento (160 blocos por padrão) veem um céu vermelho-sangue, nuvens escuras de tempestade, névoa vermelha densa, raios ao longe, clarões, cinzas vermelhas flutuando e o som da tempestade. O efeito é só visual e só para esses jogadores: o clima, o horário e os biomas do mundo **não** mudam. Tudo volta ao normal quando a criatura morre, é descarregada ou quando o jogador sai do raio.
- **Barra de vida própria:** barra carmim pulsante com uma veia de luz percorrendo.
- **Ataque de filamentos:** ela mergulha sobre o alvo e chicoteia tudo em volta com os filamentos de tempestade, com dano e repulsão.
- **Lasers dos olhos:** em 20% dos ataques, em vez do mergulho, os olhos brilham por 1 segundo (aviso) e disparam dois feixes que varrem o chão atrás do alvo por uns 3,5 segundos. Onde os feixes tocam, o chão e os arredores pegam fogo, e quem estiver ali leva dano e fica em chamas. Dá para escapar correndo. O fogo respeita a regra `mobGriefing` e pode ser desligado na config.
- **Petrificação (habilidade principal):** a cada ~30 s ela para, o céu escurece e fica mais vermelho, e energia vermelha converge para o corpo. Então ela solta um pulso. Todo ser vivo no raio (22 blocos na horizontal) fica cinza e vira pedra aos poucos, com partículas de pedra rachando e sons de rocha. Ele não consegue andar, pular, atacar, usar itens nem quebrar blocos. Ao fim do efeito, morre.
  - Um **Totem da Imortalidade** ainda salva o jogador.
  - Nenhum bloco é colocado e nenhum inventário é apagado: a morte é normal, com os itens caindo conforme as regras do mundo.
- **Morte:** espasmos, as asas caem, ela desaba em meio a raios e explode em cinzas. Dropa 1 Estrela do Nether, blocos de redstone, fragmentos de eco e 500 de XP.

Jogadores em criativo/espectador não são alvo nem são petrificados (dá para mudar isso na config).

---

## 3b. Reflexo de Cinzas (a dimensão espelhada)

Uma cópia 1:1 do seu mundo, apodrecida e presa debaixo da tempestade vermelha.

### Abrir o portal
1. No chão, deixe um espaço vazio de **4x4**.
2. Cerque esse espaço com **Blocos de Redstone**, na mesma altura: 4 de cada lado, 16 no total. Os cantos são opcionais.
3. **Jogue** uma cabeça do Steve lá dentro (tecla **Q**).

Quando a cabeça cai no chão, ela é consumida, cai um raio e o portal se abre. É só pisar nele para atravessar.

```
    R R R R
  R . . . . R       R = Bloco de Redstone
  R . . . . R       . = espaço vazio (o portal)
  R . . . . R
  R . . . . R
    R R R R
```

### Como é lá dentro
- **Mesmo mundo, mesmas coordenadas:** mesma seed, então relevo, biomas, cavernas, vilas e estruturas são idênticos. Ao entrar, você aparece no mesmo X/Z, ao lado de um portal de volta, que é construído automaticamente se ainda não existir.
- **Suas construções também estão lá, e continuam sincronizadas:** na primeira vez que um chunk do reflexo é carregado, o que foi construído ou alterado nesse chunk do mundo original é copiado. Depois disso, **toda mudança de bloco no mundo normal** (colocar, quebrar, explosões, pistões, água, plantações crescendo, comandos) é repetida no Reflexo, já apodrecida. Se o chunk do Reflexo estiver carregado, a mudança aparece em instantes; se não estiver, fica anotada e é aplicada quando ele carregar.
  - O conteúdo de baús e outros recipientes **não** é copiado, para não duplicar itens.
  - O mundo normal manda: quando uma coluna de blocos é sincronizada, ela substitui o que você tiver mudado nessa mesma coluna dentro do Reflexo. Blocos de portal abertos no Reflexo nunca são apagados.
  - A sincronização é só do mundo normal para o Reflexo, e só de blocos (não de mobs, molduras ou suportes de armadura).
- **Mundo apodrecido:** a grama vira terra grossa, podzol e manchas de nylium carmesim; flores e plantas viram arbustos secos e raízes carmesim; quase metade das folhas cai; cipós vermelhos pendem das árvores; e a vegetação que sobra fica marrom-acinzentada.
- **Atmosfera:** céu vermelho permanente com nuvens de tempestade, névoa vermelha, raios e clarões, fuligem e brasas no ar, luz avermelhada e o som da tempestade. É sempre noite.
- **Vazio e hostil:** não há animais, aldeões, peixes nem morcegos, só monstros. Mobs trazidos de propósito (ovo gerador, comando, balde, com nome, na coleira ou domados) são mantidos.
- **Voltar:** pise no portal de volta. Se quebrar um bloco de redstone de um portal aberto, o portal fecha e esse bloco não dropa nada, para os portais não virarem fonte de redstone grátis.

### Phantom X
O espectro de fuligem que caça no Reflexo de Cinzas. Aparece naturalmente **só nessa dimensão**, com frequência maior que a de zumbis e esqueletos, em grupos de 1 a 3. No mundo normal, só aparece por ovo gerador ou comando.

- **Invisível:** fica quase transparente, como um vulto tremido, deixando um rastro leve de fuligem e sussurrando. A rachadura vermelha do rosto também some.
- **Combo:** chega perto invisível, aparece de repente e dá **4 golpes rápidos** de 1,5 coração cada. Todos acertam, porque ignoram o tempo de invulnerabilidade entre golpes. Depois some de novo, recua e volta a atacar.
- **Ferido:** quando leva dano, perde a invisibilidade por um instante.
- **Status:** 40 de vida (como um Enderman), mas não é um chefe.

### Phantom Bat
Um morcego de fuligem com olhos em brasa. Voa em grupos de 2 a 4 no Reflexo de Cinzas e, bem mais raro, em grupos de 2 ou 3 no escuro do mundo normal. Também entra nas ondas do coliseu a partir da onda 3.

- **Investida:** ele chega perto, recua um instante com as asas erguidas e mergulha em linha reta com as asas para trás. Como o mergulho é reto, dá para desviar dando um passo para o lado.
- **Mordida:** 1 coração de dano (reduzido pela armadura) e o efeito **Sangramento por 5 segundos**, que tira meio coração a cada 1,25 s (2 corações no total) e ignora a armadura. Enquanto durar, gotas vermelhas de sangue caem de quem foi mordido. Leite cura.
- **Status:** 8 de vida, imune a fogo. Às vezes dropa uma membrana de phantom.

### Oceano escuro e Phantom Sea
- **Água azul-escura:** no Reflexo, a água de oceanos, rios e lagos fica num azul-marinho profundo. Debaixo d'água, a névoa é quase preta e a visibilidade cai para cerca de 20 blocos. No mundo normal, a água continua igual.
- **Phantom Sea:** um espectro afogado, parente do Phantom X, com capuz encharcado, rachadura vermelha no rosto, guelras brilhantes, braços de garras e cauda de enguia. Aparece naturalmente **só na água do Reflexo**, em grupos de 1 a 3.
  - Caça quem estiver nadando. Quando chega perto, **agarra e puxa para o fundo**, mordendo a cada segundo enquanto segura.
  - **Para se soltar:** cause 6 de dano nele enquanto ele te segura. Senão, ele solta sozinho depois de 6 segundos. Depois de soltar, demora uns 4 segundos para agarrar de novo.
  - Fora d'água ele resseca e perde vida aos poucos.
  - Tem 30 de vida.

---

## 3c. Coliseu em Ruínas, Ilhas do Céu e o Localizador

### Coliseu em Ruínas
Um anfiteatro redondo enorme (uns 87 blocos de diâmetro e até 24 de altura), destruído e coberto de vinhas: arena de areia, muro do pódio, arquibancadas em degraus sobre um corredor em anel, fachada de três andares de arcos, quatro portões, um setor inteiro desabado e entulho espalhado.

- **É único, como o portal do End:** só existe **1**, a cerca de 950 a 1.850 blocos do ponto 0,0. Ele usa o mesmo tipo de posicionamento em anel das fortalezas, com contagem 1.
- **Só existe no Reflexo de Cinzas:** no mundo normal ele não é gerado.
- **A batalha:** quando um jogador em sobrevivência pisa na areia da arena, a **Storm Ray** emerge do selo no centro junto com a primeira onda. Enquanto a arraia estiver viva e houver jogadores por perto, chega uma nova onda a cada 45 segundos:

  | Onda | Phantom X | Phantom Bat | Arraia |
  |---|---|---|---|
  | 1 e 2 | 2 | - | filamentos e lasers |
  | 3 | 3 | 2 | filamentos e lasers |
  | 4 | 3 | 2 | **começa a petrificar** |
  | 5 e 6 | 4 | 3 | tudo |
  | depois | +1 a cada duas ondas | +1 a cada duas ondas | tudo |

  Nunca há mais de 6 Phantom X e 6 Phantom Bats vivos ao mesmo tempo. Eles chegam visíveis por alguns segundos, e a arraia não os fere nem petrifica. A arraia do coliseu tem 75% da vida de uma arraia do ritual. O combate foi pensado para armadura de netherite completa: dá para vencer, mas exige atenção (os filamentos tiram cerca de 2 corações, um combo de Phantom X cerca de 1,5, e o sangramento dos morcegos ignora a armadura). Depois que a arraia morre, as ondas param e a arena descansa por 3 dias de jogo (configurável).  Não acontece no modo pacífico nem com jogadores em criativo/espectador.
- **Um guardião por vez:** a arena lembra da sua arraia. Sair e voltar não invoca outra: a mesma continua lá até ser morta.

Comando para achar:

```
/locate structure stormray:ruined_colosseum
```

### Ilhas do Céu
Pedaços de terra arrancados do chão e suspensos no ar, com a cratera de onde saíram logo abaixo. Existem **no Reflexo de Cinzas e no mundo normal** (nos mesmos lugares; no mundo normal são de terra e grama, no Reflexo são apodrecidas e com cipós vermelhos), entre 45 e 70 blocos acima do solo, e são de raridade média (em média uma a cada ~380 blocos, um pouco mais comuns que vilas). Em cima há um pequeno santuário em ruínas com um **baú**, que sempre contém um **Localizador do Coliseu**, tem **20% de chance** de trazer uma **Potion of Stone Breaker** e mais alguns itens (redstone, ossos, minérios, às vezes uma cabeça do Steve). No Reflexo, luzes alaranjadas na parte de baixo ajudam a enxergar a ilha através da névoa.

```
/locate structure stormray:sky_island
```

### Localizador do Coliseu
Clique com o botão direito: um rastro de brasas aponta a direção do coliseu e uma mensagem mostra a distância e o ponto cardeal. Não é consumido. O coliseu fica só no Reflexo, mas as coordenadas são as mesmas: usado no mundo normal, o localizador mostra para onde ir antes de atravessar o portal. No criativo ele fica na aba **Ferramentas e Utilidades**.

### Potion of Stone Breaker
Uma poção com o visual normal do Minecraft, mas com o líquido **preto**. Dá o efeito **Stone Breaker por 8 minutos**: enquanto durar, o pulso de petrificação da Storm Ray não transforma você em pedra (a pedra racha e cai). Enquanto vira pedra você não consegue usar nada, **exceto esta poção**: se terminar de beber antes de virar pedra por completo (a petrificação leva 3 segundos e beber leva 1,6), a pedra racha e você se salva. A versão arremessável funciona na hora. Só é encontrada nos baús das Ilhas do Céu (20% de chance por baú). Para testes: `/give @s minecraft:potion{Potion:"stormray:stone_breaker"}` ou `/effect give @s stormray:stone_breaker 480`.

> **Mundos já existentes:** estruturas só são geradas em chunks novos. Se a região do Reflexo onde o coliseu deveria ficar já tiver sido explorada, ele não aparece ali; nesse caso use um mundo novo. O mesmo vale para as ilhas em regiões já visitadas. Um coliseu que já tenha sido gerado no mundo normal por uma versão anterior continua lá como construção, mas não invoca mais nada.

---

Limitações conhecidas: na primeira visita as construções copiadas podem "aparecer" alguns segundos depois de o chunk carregar. Nos chunks do Reflexo ocupados pelo coliseu ou por uma ilha do céu, as construções do mundo original não são copiadas.

---

## 4. Configuração

Arquivos gerados na primeira execução:

- `config/stormray-common.toml` (servidor/mundo; em mundos single player fica na pasta `config` do jogo)
- `config/stormray-client.toml` (visual, para cada jogador)

| Seção | Opção | Padrão | O que faz |
|---|---|---|---|
| boss | `health` | 900 | Vida base. Acima de 1024 (limite do vanilla) é simulada com redução de dano |
| boss | `armor` | 14 | Armadura |
| boss | `tentacleDamage` / `tentacleReach` / `tentacleCooldownTicks` | 14 / 9 / 90 | Ataque de filamentos |
| boss | `detectionRadius` | 128 | Distância para detectar jogadores |
| boss | `hoverHeight` | 14 | Altura em que paira sobre o alvo |
| boss | `difficulty` | NORMAL | `CASUAL`, `NORMAL`, `HARD` ou `NIGHTMARE` (multiplica vida, dano e frequência das habilidades) |
| summoning | `enableRitual` | true | Liga/desliga o ritual |
| summoning | `requireSteveHead` | true | Exige a cabeça do Steve |
| summoning | `summonDurationTicks` | 140 | Duração da emergência |
| summoning | `allowMultipleBosses` / `minDistanceBetweenBosses` | false / 256 | Evita várias ao mesmo tempo |
| environment | `radius` | 160 | Raio do céu vermelho, da névoa e da barra de vida |
| environment | `lightning` / `lightningAverageIntervalTicks` | true / 60 | Raios visuais (não causam fogo nem dano) |
| petrification | `radius` | 22 | Raio do pulso |
| petrification | `chargeTicks` | 80 | Tempo de carga |
| petrification | `petrifyTicks` | 60 | Tempo até virar pedra por completo |
| petrification | `cooldownTicks` | 600 | Intervalo entre pulsos |
| petrification | `instantKill` / `damage` | true / 30 | Matar ou só causar dano |
| petrification | `affectPlayers` / `affectMobs` / `affectCreativePlayers` | true / true / false | Quem é afetado |
| dimension | `enablePortal` | true | Liga/desliga o portal |
| dimension | `copyBuilds` | true | Copia as construções do mundo original no primeiro carregamento de cada chunk |
| dimension | `skyIslandsInOverworld` | true | As Ilhas do Céu também aparecem no mundo normal |
| dimension | `liveSync` | true | Repete no Reflexo toda mudança de bloco feita depois no mundo normal |
| dimension | `syncColumnsPerTick` | 64 | Limite de colunas de blocos sincronizadas por tick |
| dimension | `hostileOnly` | true | Só monstros no reflexo |
| dimension | `syncChunksPerTick` / `syncMillisPerTick` | 4 / 8 | Limite de trabalho da cópia por tick (desempenho) |
| dimension | `lightningAverageIntervalTicks` | 90 | Frequência dos raios vermelhos no reflexo |
| phantom_x | `health` / `hitDamage` / `comboHits` / `comboCooldownTicks` | 40 / 3 / 4 / 50 | Força do Phantom X |
| phantom_x | `naturalSpawnWeight` | 160 | Frequência de spawn no Reflexo (zumbi = 95; 0 desliga) |
| phantom_sea | `health` / `biteDamage` / `pullStrength` / `grabDurationTicks` / `breakFreeDamage` | 30 / 3 / 0,2 / 120 / 6 | Força e agarrão do Phantom Sea |
| phantom_bat | `health` / `biteDamage` / `bleedTicks` | 8 / 2 / 100 | Força do Phantom Bat e duração do sangramento |
| phantom_bat | `naturalSpawnWeight` / `overworldSpawnWeight` | 130 / 20 | Frequência no Reflexo e no mundo normal (zumbi = 95; 0 desliga) |
| phantom_sea | `naturalSpawnWeight` | 250 | Frequência de spawn na água do Reflexo (0 desliga) |
| colosseum | `awakenBoss` | true | A Storm Ray emerge quando um jogador entra na arena do coliseu |
| colosseum | `respawnTicks` | 72000 | Descanso da arena depois da morte da arraia (0 = a luta só acontece uma vez) |
| colosseum | `bossHealthMultiplier` | 0,75 | Vida da arraia do coliseu em relação à do ritual |
| colosseum | `firstWavePhantoms` / `maxPhantomX` | 2 / 6 | Phantom X na primeira onda (+1 a cada duas ondas) e máximo vivo |
| colosseum | `batsFromWave` / `maxBats` | 3 / 6 | Onda em que os Phantom Bats entram e máximo vivo |
| colosseum | `ticksBetweenWaves` | 900 | Intervalo entre as ondas (0 desliga as ondas extras) |
| colosseum | `petrifyFromWave` | 4 | Onda a partir da qual a arraia do coliseu usa a petrificação (não afeta arraias do ritual) |
| laser | `chance` / `damage` / `setFire` | 0,2 / 4 / true | Chance de o ataque ser o laser dos olhos, dano por acerto e se o chão pega fogo |
| client | `skyTintStrength`, `screenTintStrength`, `thickFog`, `lightningFlashes`, `ambientParticles`, `ambienceVolume` | | Intensidade visual e sonora. `lightningFlashes = false` desliga os clarões de tela (fotossensibilidade) |

20 ticks = 1 segundo.

---

## 5. Desenvolvimento (IntelliJ IDEA)

1. **File → Open** e escolha a pasta do projeto (a que tem o `build.gradle`).
2. Aceite **Load Gradle Project**. Configure o **Gradle JVM** como **Java 17** (Settings → Build Tools → Gradle).
3. Espere a primeira sincronização (o Forge baixa e descompila o Minecraft; leva alguns minutos).
4. No terminal do IntelliJ: `./gradlew genIntellijRuns` e depois recarregue o Gradle.
5. Rode a configuração **runClient** para abrir o jogo com o mod.

Comandos úteis:

```bash
./gradlew build        # compila, roda os testes e gera build/libs/stormray-1.20.1-1.6.0.jar
./gradlew test         # só os testes
./gradlew runClient    # abre o jogo em modo de desenvolvimento
```

### Assets

Todos os assets são originais e gerados por código:

- `tools/generate_assets.py`: modelo GeckoLib (46 bones), 7 animações, textura + glowmask, partículas, nuvens e overlays de pedra.
- `tools/generate_sounds.py`: 33 efeitos sonoros sintetizados (sem samples de terceiros).

Antes de compilar, `python3 tools/validate_animations.py` confere as animações com as mesmas regras que o GeckoLib usa ao carregar o jogo (o GitHub Actions também roda essa checagem). Para regerar: `pip install numpy pillow scipy`, ter o `ffmpeg` instalado e rodar `python3 tools/generate_assets.py` e `python3 tools/generate_sounds.py`. O modelo (`src/main/resources/assets/stormray/geo/entity/storm_ray.geo.json`) abre no **Blockbench** (plugin GeckoLib). Para trocar por um modelo feito à mão, mantenha os mesmos nomes de bones e de animações (`animation.storm_ray.idle`, `summon`, `swoop`, `lash`, `charge`, `release`, `death`).

---

## 6. Publicar no GitHub e compilar com GitHub Actions

### 6.1 Criar o repositório
1. Entre em <https://github.com/new>.
2. Nome: `storm-ray` (ou outro). Público ou privado.
3. **Não** marque "Add a README", nem .gitignore, nem licença (o projeto já tem). Clique em **Create repository**.

### 6.2 Enviar o código

> **Atenção:** o projeto tem pastas e arquivos que começam com ponto (`.github`, `.gitignore`, `.gitattributes`). O macOS e o Windows escondem esses itens, e o upload por arrastar no site costuma deixá-los de fora. Sem a pasta `.github` o build automático não roda. Por isso, use uma das opções abaixo.

**Opção A — GitHub Desktop (mais fácil)**
1. Instale o GitHub Desktop e faça login.
2. **File → Add local repository**, escolha a pasta extraída do ZIP. Quando ele avisar que não é um repositório, clique em **create a repository** e confirme.
3. Escreva uma mensagem no campo *Summary* (ex.: "Primeira versão") e clique em **Commit to main**.
4. Clique em **Publish repository** e escolha o nome/visibilidade.

**Opção B — terminal (git)**
```bash
cd storm-ray
git init
git add .
git commit -m "Storm Ray: primeira versão"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/storm-ray.git
git push -u origin main
```

### 6.3 Primeiro build
1. No repositório, abra a aba **Actions**. Se pedir, clique em **I understand my workflows, go ahead and enable them**.
2. O workflow **Build Storm Ray** roda sozinho a cada push (ou manualmente em **Run workflow**). Ele:
   instala o Java 17 → configura o Gradle → baixa as dependências do Forge → compila → roda os testes → gera o JAR → publica o JAR como artefato.
3. O primeiro build leva ~8–15 min (o Forge prepara o Minecraft); os seguintes usam cache.
4. Quando ficar verde ✅, abra a execução e baixe o artefato **storm-ray-mod** no fim da página. É um `.zip` com o `stormray-1.20.1-1.6.0.jar` dentro.
5. Se ficar vermelho ❌, abra o passo que falhou e copie o erro. Os relatórios de teste ficam no artefato **test-reports**.

---

## 7. Estrutura do projeto

```
src/main/java/com/stormray/
├── StormRayMod.java              # @Mod, registros, config, rede
├── config/                       # StormRayConfig (comum), StormRayClientConfig, DifficultyPreset
├── registry/                     # entidades, itens (ovo), sons, partículas, damage type
├── ritual/                       # forma do T + detecção/consumo do ritual
├── entity/                       # Storm Ray, Phantom X e Phantom Sea (IA, ataques, GeckoLib) e spawn natural
├── petrify/                      # petrificação (servidor), espelho no cliente, bloqueio de ações
├── storm/                        # evento do céu vermelho no servidor (sincronização, raios)
├── dimension/                    # Reflexo de Cinzas: apodrecimento, cópia de chunks, regras de mobs
├── portal/                       # portal 4x4: ignição pela cabeça jogada, bloco do portal, viagem
├── structure/                    # Coliseu em Ruínas e Ilhas do Céu (forma pura testada, peças, luta da arena)
├── item/                         # Localizador do Coliseu
├── effect/                       # efeito Stone Breaker (imunidade à petrificação)
├── network/                      # canal + pacotes servidor→cliente
├── util/                         # matemática pura (testada)
└── client/                       # renderer GeckoLib, céu/névoa, overlays, boss bar, partículas, som, efeitos da dimensão
src/main/resources/
├── assets/stormray/              # geo, animações, texturas, sons, partículas, portal, idiomas (en_us, pt_br)
└── data/                         # dimensão, tipo de dimensão, feature de apodrecimento, biome modifier, damage type, tags, loot, receita
src/test/java/                    # testes JUnit 5 (ritual, matemática, dificuldade, integridade de assets)
tools/                            # geradores de assets e sons
.github/workflows/build.yml       # CI
```

### Arquitetura
- **Autoridade do servidor:** movimento, alvos, ataques, petrificação, dano e morte rodam só no servidor.
- **Sincronização:** fase do boss via `SynchedEntityData`; intensidade do céu, clarões e progresso da petrificação via pacotes. As animações de ataque são disparadas pelo sistema de triggers do GeckoLib.
- **Cliente:** renderiza a partir desses valores e volta ao normal sozinho se as atualizações pararem (sem estado preso).

---

## 8. Status de testes

- Os testes unitários (`src/test`) passam: formato do ritual e do portal, forma do coliseu e das ilhas do céu, matemática do céu, ruído determinístico do apodrecimento, presets de dificuldade e integridade dos assets e dados (todo som, sprite, bone e arquivo de dimensão referenciado existe).
- A compilação completa contra o Forge e o GeckoLib acontece no GitHub Actions. Abra uma issue com o log se algo falhar.
