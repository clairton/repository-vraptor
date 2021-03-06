# repository-http [![Build Status](https://travis-ci.org/clairton/repository-http.svg?branch=master)](https://travis-ci.org/clairton/repository-http)

Convert url para predicates de [Repository](https://github.com/clairton/repository).

Segue exemplos:
```java
http://meudominio.com/app/recurso?nome=abc //retornara o recurso com o nome igual a "abc"

http://meudominio.com/app/recurso?operacao.nome=abc //retornara o recurso com o nome da operacao igual a "abc"

http://meudominio.com/app/recurso?operacao[nome]=abc //retornara o recurso com o nome da operacao igual a "abc"

http://meudominio.com/app/recurso?nome[]=abc&nome[]=def //retornara o recurso com o nome igual a "abc" ou "def"

http://meudominio.com/app/recurso?id=>=1&id=<=11 //retornara o recurso com o id entre 1 e 11

http://meudominio.com/app/recurso?page=2&per_page=10 //retornara a segunda pagina com 10 itens

http://meudominio.com/app/recurso?sort=operacao.id&direction=ASC//ordenara ascendentemente pelo id da operação

http://meudominio.com/app/recurso?sort=operacao.id&direction=DESC//ordenara decrescente pelo id da operação

http://meudominio.com/app/recurso?sort[]=id&sort[]=operacao.id//ordenara pelo id e pelo id da operação
```
Se for informado somente a opção "sort", "direction" assume ASC.

Como pode notar a formato é o seguinte "nomeDoCampo=[operacaoLogica]valorDoFiltro", a operação lógica
não é obrigatório, sendo que se não for informada é assumida como "igual".
As operações lógicas disponíveis são:
* == Igual
* =* Igual ignorando maisculas e minusculas
* * Contém
* !* Não Contém
* ^ Começa Com
* $ Termina Com
* !^ Não Começa Com
* !$ Não Termina Com
* <> Diferente
* ∃  Existe
* ∅  Nulo
* !∅ Não Nulo
* &gt; Maior
* &gt;= Maior ou Igual
* &lt; Menor
* &lt;= Menor ou Igual

Para recuperar no lador do servidor:
```java
@Inject QueryParser queryParser;
@Inject ServletRequest request;
@Inject Repository repository;
Class<?> modelType = Aplicacao.class;
...
Page page = queryParser.paginate(request, modelType);
Collection<Predicate> predicates = queryParser.parse(request, modelType);
List<Order> orders = queryParser.order(request, modelType);
PaginatedCollection<Aplicacao, Meta> collection = repository.from(modelType)
							.distinct()
							.where(predicates)
							.orderBy(orders)
							.collection(paginate.offset, paginate.limit);
```

Para usar será necessário adicionar os repositórios maven:

 Também adicionar as depêndencias:
```xml
<dependency>
    <groupId>br.eti.clairton</groupId>
	<artifactId>repository-http</artifactId>
	<version>lastversion</version>
</dependency>
```
