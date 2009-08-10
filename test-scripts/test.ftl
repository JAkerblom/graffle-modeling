Hello World

Triples follow:
<#list query("template-test") as triple>
    ${triple.a} --(${triple.prop})--> ${triple.b}
</#list>