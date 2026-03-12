require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name             = "NitroTelegramTabBar"
  s.version          = package["version"]
  s.summary          = package["description"]
  s.homepage         = "https://github.com/milautonomos/react-native-telegram-tabbar"
  s.license          = { :type => "MIT" }
  s.author           = package["author"]
  s.source           = { :git => "https://github.com/milautonomos/react-native-telegram-tabbar.git", :tag => s.version.to_s }

  s.platforms        = { :ios => "16.0" }
  s.swift_version    = "5.9"
  s.requires_arc     = true

  # Hand-written Swift implementation
  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.dependency "React-Core"

  s.pod_target_xcconfig = {
    "SWIFT_VERSION" => "5.9",
  }
end
