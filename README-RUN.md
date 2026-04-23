# Como rodar rapidamente

## Opcao mais facil (1 clique)

No Windows, execute o arquivo:

`rodar-roadgraph.bat`

Ele faz:

1. Compila o projeto (`javac`)
2. Mostra menu com os mapas `.osm` disponiveis
3. Executa o app (`java -cp "target\classes" GamePanel <mapa>`)

## Atalho no Desktop (2 cliques para abrir)

Execute uma vez:

`criar-atalho-desktop.bat`

Ele cria no seu Desktop o atalho `RoadGraph A Star.lnk`.
Depois disso, e so abrir o atalho para iniciar o launcher.

## Gerar JAR executavel (2 cliques)

Execute:

`gerar-jar.bat`

Isso cria:

`dist\RoadGraph-AStar.jar`

Para usar por 2 cliques:

1. Copie o `RoadGraph-AStar.jar` para uma pasta
2. Coloque os arquivos `.osm` na mesma pasta
3. Dê duplo clique no JAR (ele abre um dialogo para escolher o mapa)

## Pre-requisito

- Ter o **JDK** instalado e configurado no `PATH` (comandos `javac` e `java` disponiveis).
