job("task6_job1"){
  description("Pull files from github repo when some developers push code to github")
  scm{
    github("Jasprit-Kaur/devops_code6","master")
  }
  triggers {
    scm("* * * * *")
  }
  steps{
    shell('''if ls / | grep task6
then
sudo cp -rvf * /task6
else
sudo mkdir /task6
sudo cp -rvf * /task6
fi  
''')
  }
}

job('task6_job2'){
description("Second Job: Deploying respective webpages on the server")


triggers {  
upstream('task6_job1', 'SUCCESS')
}
steps{
remoteShell('root@192.168.43.176:22') {
command('''if sudo ls /root/task6 | grep .php
then
if sudo kubectl get deployment | grep phpserver
then
echo "The PHP Deployment is already running"
else
sudo kubectl create -f /root/phpserver.yml
sleep 6
if kubectl get pods | grep php
then
b=$(sudo kubectl get pods -o 'jsonpath={.items[0].metadata.name}')
sudo kubectl cp /root/task6/index.php $b:/var/www/html
else
echo "Cannot copy the PHP code"
fi
fi
else
echo "Sorry PHP code not found"
fi''')
}
}
}

job("task6_job3"){
description("Third Job: Environment testing")


triggers {
upstream('task6_job2','SUCCESS')
}
steps{
remoteShell('root@192.168.43.176:22') {
command('''if sudo kubectl get pods | grep phpserver
then
php_status_code=$(curl -o /dev/null -s -w "%{http_code}" 192.168.99.100:32000)
if [[ $php_status_code == 200 ]]
then
echo "The PHP server is working fine"
else
echo "Something is wrong with PHP server"
exit 1
fi
else
echo "No PHP server running"
fi''')
}
}

publishers {
extendedEmail {
recipientList('jaspritkaur1603@gmail.com')
defaultSubject('Build failed')
defaultContent('The testing has been failed. Please Check it!!')
contentType('text/html')
triggers {
beforeBuild()
stillUnstable {
subject('Subject')
content('Body')
sendTo {
developers()
requester()
culprits()
}
}
}
}
}
}

buildPipelineView("task6_pipeline") {
    filterBuildQueue(true)
    filterExecutors(false)
    title("Task6")
    displayedBuilds(1)
    selectedJob("task6_job1")
    alwaysAllowManualTrigger(true)
    showPipelineParameters(true)
    refreshFrequency(5)
}

