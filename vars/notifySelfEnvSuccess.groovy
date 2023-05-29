def call(Map config) {
      sendMail2 (
        subject: "您的自测环境已经准备好了",
        body: """您的自测环境如下: <p><font size=72 color="green">${config.serverinfo}</font></p><br><br>当你完成测试的时候，请使用下面的命令来移除:

<pre>
curl -X POST http://runner:7589e70507542e2da9e00330a497384c@ci.rd.rightcloud.com.cn/job/rightcloud-server-self-test-remove/build -d json='{"parameter":[{"name": "EMAIL_ADDRESS", "value":"${config.mail}"}]}'
</pre>

使用<font color="red" size=72>POSTMAN</font>的话，使用下面的配置:

<style media="screen" type="text/css">
table, th, td {
    border: 1px solid black;
    border-collapse: collapse;
}
</style>

Post to URL: http://ci.rd.rightcloud.com.cn/job/rightcloud-server-self-test-remove/build
<br>

<b>Headers</b>:
<pre>
<table>
<tr>
<th>key</th>
<th>value</th>
</tr>
<tr>
<td>Jenkins-Crumb</td>
<td>7d04702e4b895760fe254eadab918c18</td>
</tr>
<tr>
<td>Content-Type</td>
<td>application/x-www-form-urlencoded</td>
</tr>
<tr>
<td>Authorization</td>
<td>Basic cnVubmVyOjc1ODllNzA1MDc1NDJlMmRhOWUwMDMzMGE0OTczODRj</td>
</tr>
</table>
</pre>

<br>
<b>body</b>: <br>

data type: x-www-form-urlencoded

<pre>
<table>
<tr>
<th>key</th>
<th>value</th>
</tr>
<tr>
<td>json</td>
<td>{"parameter":[{"name": "EMAIL_ADDRESS", "value":"${config.mail}"}]}</td>
</tr>
</table>
</pre>

""",
        to: config.mail,
        cc: '',
        attachLog: false
    );
}
