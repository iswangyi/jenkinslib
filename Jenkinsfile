@Library('mylib') _     
def mytools = new org.devops.tools()
pipeline {
 
	agent { label  "build" }
 
	stages {
		stage("build"){
			steps{
				script{
					msg = "hello jenkins"
					mytools.PrintMsg(msg)
				}
			}
		}
	}
}
