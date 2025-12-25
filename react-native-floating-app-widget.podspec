require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-floating-app-widget"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => package["repository"], :tag => "#{s.version}" }

  # This library is Android-only, but we need a podspec for iOS compatibility
  # The podspec does nothing on iOS
  s.source_files = "ios/**/*.{h,m,mm}"

  s.dependency "React-Core"
end
